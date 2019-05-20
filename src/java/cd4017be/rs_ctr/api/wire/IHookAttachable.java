package cd4017be.rs_ctr.api.wire;

import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author CD4017BE
 *
 */
public interface IHookAttachable extends ISignalIO {

	Int2ObjectMap<RelayPort> getHookPins();

	public static int getAttachmentPos(Vec3d target, EnumFacing side, EntityPlayer player) {
		target = target.scale(4.0).add(new Vec3d(side.getDirectionVec()).scale(-0.125));
		int x0 = (int)Math.floor(target.x), y0 = (int)Math.floor(target.y), z0 = (int)Math.floor(target.z);
		switch(side) {
		case DOWN: case UP: target = new Vec3d(target.x, (double)(y0 + 0.5), target.z); break;
		case NORTH: case SOUTH: target = new Vec3d(target.x, target.y, (double)(z0 + 0.5)); break;
		default: target = new Vec3d((double)(x0 + 0.5), target.y, target.z); break;
		}
		target = target.subtract(player.getLook(1).scale(0.5));
		int x1 = (int)Math.floor(target.x), y1 = (int)Math.floor(target.y), z1 = (int)Math.floor(target.z);
		x1 = MathHelper.clamp(x0 - x1 - side.getFrontOffsetX(), -1, 1);
		y1 = MathHelper.clamp(y0 - y1 - side.getFrontOffsetY(), -1, 1);
		z1 = MathHelper.clamp(z0 - z1 - side.getFrontOffsetZ(), -1, 1);
		int p = (x0 - x1 & 0xf) | (y0 - y1 & 0xf) << 4 | (z0 - z1 & 0xf) << 8;
		return p | (x1 + 2) << 16 | (y1 + 2) << 18 | (z1 + 2) << 20;
	}

	default int applyOrientation(int pin) {
		Orientation o = getOrientation();
		if (o == Orientation.N) return pin;
		Vec3d vec = o.invRotate(new Vec3d(pin & 3, pin >> 4 & 3, pin >> 8 & 3).addVector(-1.5, -1.5, -1.5));
		Vec3d vec1 = o.invRotate(new Vec3d(pin >> 16 & 3, pin >> 18 & 3, pin >> 20 & 3).addVector(-2.0, -2.0, -2.0));
		return (int)(vec.x + 1.5) | (int)(vec.y + 1.5) << 4 | (int)(vec.z + 1.5) << 8 |
				(int)(vec1.x + 2.0) << 16 | (int)(vec1.y + 2.0) << 18 | (int)(vec1.z + 2.0) << 20;
	}

	default boolean doAttachHook(int pin) {
		if ((pin & 0x333) != (pin & 0xffff)) return false;
		Int2ObjectMap<RelayPort> pins = getHookPins();
		if (pins.containsKey(0x8000 | pin & 0xfff)) return false;
		RelayPort port = new RelayPort(this, pin);
		pins.put(port.pin, port);
		port.orient(getOrientation());
		port = port.opposite;
		pins.put(port.pin, port);
		port.orient(getOrientation());
		onPortModified(port, E_HOOK_ADD);
		return true;
	}

	default boolean removeHook(int pin, EntityPlayer player) {
		Int2ObjectMap<RelayPort> pins = getHookPins();
		RelayPort port;
		World world = null; BlockPos pos = null;
		if ((port = pins.remove(pin & 0xfff | 0x8000)) != null) {
			port.setConnector(null, player);
			world = port.getWorld();
			pos = port.getPos();
		}
		if ((port = pins.remove(pin & 0xfff | 0x9000)) != null) {
			port.setConnector(null, player);
			world = port.getWorld();
			pos = port.getPos();
		}
		if (world == null || pos == null) return false;
		if (player == null)
			ItemFluidUtil.dropStack(new ItemStack(RelayPort.HOOK_ITEM), world, pos);
		else if (!player.isCreative())
			ItemFluidUtil.dropStack(new ItemStack(RelayPort.HOOK_ITEM), player);
		onPortModified(null, E_HOOK_REM);
		return true;
	}

	default NBTTagCompound storeHooks() {
		Int2ObjectMap<RelayPort> pins = getHookPins();
		if (pins.isEmpty()) return null;
		NBTTagCompound nbt = new NBTTagCompound();
		for (Entry<Integer, RelayPort> e : pins.entrySet())
			nbt.setTag(Integer.toHexString(e.getKey()), e.getValue().serializeNBT());
		return nbt;
	}

	default void loadHooks(NBTTagCompound nbt) {
		Int2ObjectMap<RelayPort> pins = getHookPins();
		pins.clear();
		Orientation o = getOrientation();
		for (String key : nbt.getKeySet())
			try {
				int pin = Integer.parseInt(key, 16);
				if (pin != (pin & 0x9333)) continue;
				RelayPort port = pins.get(pin);
				if (port == null) {
					port = new RelayPort(this, pin);
					pins.put(port.pin, port);
					pins.put(port.opposite.pin, port.opposite);
					if ((pin & 0x1000) != 0) port = port.opposite;
				}
				port.deserializeNBT(nbt.getCompoundTag(key));
				port.orient(o);
			} catch (NumberFormatException e) {}
	}

	default Orientation getOrientation() {
		return Orientation.N;
	}

	public static final int E_HOOK_ADD = 256, E_HOOK_REM = 512;

	/**
	 * utility method for implementing {@link Block#collisionRayTrace()}
	 * @param pre existing RayTraceResult from previous code (if any)
	 * @param world
	 * @param pos
	 * @param start
	 * @param end
	 * @return the new RayTraceResult that also accounts for wire hooks
	 */
	public static RayTraceResult addBlockRayTrace(@Nullable RayTraceResult pre, World world, BlockPos pos, Vec3d start, Vec3d end) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof IHookAttachable)) return pre;
		Vec3d start1 = start.subtract(pos.getX(), pos.getY(), pos.getZ());
		Vec3d dir = (pre != null ? pre.hitVec : end).subtract(start);
		Pair<Vec3d, EnumFacing> res = null;
		for (RelayPort port : ((IHookAttachable)te).getHookPins().values()) {
			Pair<Vec3d, EnumFacing> r = port.rayTrace(start1, dir);
			if (r != null) {
				res = r;
				dir = r.getLeft();
			}
		}
		if (res != null)
			return new RayTraceResult(res.getLeft().add(start), res.getRight(), pos);
		return pre;
	}

}
