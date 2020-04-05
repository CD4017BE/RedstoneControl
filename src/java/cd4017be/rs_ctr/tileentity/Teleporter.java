package cd4017be.rs_ctr.tileentity;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.DimPos;
import cd4017be.lib.util.MovedBlock;
import static cd4017be.lib.util.MovedBlock.cut;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants.NBT;

/** @author cd4017be */
public class Teleporter extends WallMountGate implements SignalHandler, ITickableServerOnly {

	protected static final int SINGLE_BLOCK = 1, DO_TELEPORT = 8, INVALID_COORDS = -1, OUT_OF_WORLD = -2, COMPLETE = 100;
	public static double ENERGY_PER_BLOCK = 1000, MAX_DISTANCE = 1024;
	SignalHandler out = SignalHandler.NOP;
	EnergyHandler energy = EnergyHandler.NOP;
	BlockReference dest, ref;
	int action, result;
	long buffer;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false)
			.setName("port.rs_ctr.p1").setLocation(.875, .625, .25, EnumFacing.NORTH),
			new MountedPort(this, 1, BlockHandler.class, false)
			.setName("port.rs_ctr.p0").setLocation(.125, .625, .25, EnumFacing.NORTH),
			new MountedPort(this, 2, SignalHandler.class, false)
			.setName("port.rs_ctr.tp").setLocation(.625, .25, .125, EnumFacing.UP),
			new MountedPort(this, 3, SignalHandler.class, true)
			.setName("port.rs_ctr.status").setLocation(.125, .25, .125, EnumFacing.UP),
			new MountedPort(this, 4, EnergyHandler.class, true)
			.setName("port.rs_ctr.energy_i").setLocation(.375, .25, .125, EnumFacing.UP),
		};
	}

	@Override
	public void update() {
		if ((action & DO_TELEPORT) == 0) return;
		action ^= DO_TELEPORT;
		out.updateSignal(result = run());
	}

	protected int run() {
		if (dest == null || ref == null) return INVALID_COORDS;
		DimPos posA = new DimPos(ref.pos, ref.dim), posB = new DimPos(dest.pos, dest.dim);
		if (!(world.isValid(posA) && world.isValid(posB))) return OUT_OF_WORLD;
		double distance = Math.sqrt(posA.distanceSq(posB));
		if (distance == 0) return COMPLETE;
		if (distance > MAX_DISTANCE) distance = MAX_DISTANCE;
		long needed = Math.round(distance * ENERGY_PER_BLOCK);
		return initTeleport(
			()-> {
				if (unloaded) return;
				buffer -= needed;
				swap(posA, posB, null);
				List<Entity> entitiesA = getEntities(posA, null, false);
				List<Entity> entitiesB = getEntities(posB, null, false);
				moveEntities(entitiesA, posA, posB);
				moveEntities(entitiesB, posB, posA);
			}, needed
		);
	}

	protected int initTeleport(Runnable task, long needed) {
		buffer -= energy.changeEnergy((int)Math.max(buffer - needed, -Integer.MAX_VALUE), false);
		if (buffer < needed) {
			action |= DO_TELEPORT;
			return 1 + (int)(buffer * 99 / needed);
		}
		((WorldServer)world).getMinecraftServer().futureTaskQueue.add(new FutureTask<Void>(task, null));
		return COMPLETE;
	}

	protected static void swap(DimPos pa, DimPos pb, HashMap<DimPos, NBTTagCompound> addedTiles) {
		World wa = pa.getWorldServer(), wb = pb.getWorldServer();
		if (wa.getBlockState(pa).getBlockHardness(wa, pa) < 0) return;
		if (wb.getBlockState(pb).getBlockHardness(wb, pb) < 0) return;
		MovedBlock a = cut(pa, addedTiles);
		MovedBlock b = cut(pb, addedTiles);
		a.paste(pb, addedTiles);
		b.paste(pa, addedTiles);
	}

	protected static List<Entity> getEntities(DimPos pos, BlockPos size, boolean skipInner) {
		AxisAlignedBB box = size == null ? new AxisAlignedBB(pos)
			: new AxisAlignedBB(pos, pos.add(size));
		Predicate<Entity> filter = EntitySelectors.NOT_SPECTATING;
		if (skipInner) {
			AxisAlignedBB inner = box.grow(-1, 0, -1);
			filter = filter.and((e)-> !e.getEntityBoundingBox().intersects(inner));
		}
		return pos.getWorldServer().getEntitiesInAABBexcluding(null, box, filter::test);
	}

	protected static void moveEntities(List<Entity> list, BlockPos from, DimPos to) {
		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		double dz = to.getZ() - from.getZ();
		for (Entity e : list)
			MovedBlock.moveEntity(e, to.dimId, e.posX + dx, e.posY + dy, e.posZ + dz);
	}

	@Override
	public Object getPortCallback(int pin) {
		return pin == 1 ? (BlockHandler)(block) -> ref = block
			: pin == 0 ? (BlockHandler)(block) -> dest = block
			: this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if (pin == 3)
			out = callback instanceof SignalHandler ? (SignalHandler)callback : SignalHandler.NOP;
		else
			energy = callback instanceof EnergyHandler ? (EnergyHandler)callback : EnergyHandler.NOP;
	}

	@Override
	protected void resetPin(int pin) {
		switch(pin) {
		case 0: dest = null; break;
		case 1: ref = new BlockReference(world, pos, o.front); break;
		case 2: updateSignal(0); break;
		}
	}

	@Override
	public void updateSignal(int value) {
		value &= DO_TELEPORT - 1;
		if (action == 0 && value != 0) value |= DO_TELEPORT;
		else if (value == 0 && action != 0 && result != 0)
			out.updateSignal(result = 0);
		action = value;
		markDirty(SAVE);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		nbt.setLong("buff", buffer);
		nbt.setByte("op", (byte)action);
		nbt.setByte("res", (byte)result);
		if (dest != null) nbt.setTag("dst", dest.serializeNBT());
		if (ref != null) nbt.setTag("src", ref.serializeNBT());
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		buffer = nbt.getLong("buff");
		action = nbt.getByte("op");
		result = nbt.getByte("res");
		dest = nbt.hasKey("dst", NBT.TAG_COMPOUND) ? new BlockReference(nbt.getCompoundTag("dst")) : null;
		ref = nbt.hasKey("src", NBT.TAG_COMPOUND) ? new BlockReference(nbt.getCompoundTag("src")) : null;
	}

	@Override
	protected void orient(Orientation o) {
		super.orient(o);
		if (((MountedPort)ports[1]).getConnector() == null && world != null && !world.isRemote)
			resetPin(1);
	}

	@Override
	public void setWorld(World world) {
		super.setWorld(world);
		if (((MountedPort)ports[1]).getConnector() == null && world != null && !world.isRemote)
			resetPin(1);
	}

	@Override
	public Object getState(int id) {
		switch(id) {
		case 0: return dest;
		case 1: return ref;
		case 2: return action & DO_TELEPORT - 1;
		case 3: return result;
		default: return null;
		}
	}

}
