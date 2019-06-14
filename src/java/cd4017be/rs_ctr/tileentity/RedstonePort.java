package cd4017be.rs_ctr.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

import org.apache.commons.lang3.ArrayUtils;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.Cover;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;

import static cd4017be.rs_ctr.api.signal.MountedSignalPort.SIZE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;


/**
 * @author CD4017BE
 *
 */
public class RedstonePort extends Gate implements IRedstoneTile, INeighborAwareTile, IUpdatable, IModularTile, ITilePlaceHarvest {

	IntConsumer[] callbacks = new IntConsumer[6];
	/**0-5: input, 6-11: output */
	final int[] states = new int[12];
	byte strong, dirty;
	public Cover cover = new Cover();

	{ports = new MountedSignalPort[0];}

	@Override
	public SignalPort getSignalPort(int pin) {
		SignalPort port = super.getSignalPort(pin);
		if (port != null) return port;
		for (MountedSignalPort p : ports)
			if (p.pin == pin)
				return p;
		return port;
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
		return new RSOut(pin);
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		IntConsumer c = callback instanceof IntConsumer ? (IntConsumer)callback : null;
		callbacks[pin] = c;
		if (c != null) c.accept(states[pin]);
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).accept(0);
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		int i = side.ordinal();
		return !strong || (this.strong >> i & 1) != 0 ? states[i+6] : 0;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		int i = side.ordinal();
		return getSignalPort(i) != null || getSignalPort(i + 6) != null;
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		EnumFacing side = Utils.getSide(src, pos);
		if (side != null)
			updateInput(side);
		else for (EnumFacing s : EnumFacing.VALUES)
			updateInput(s);
	}

	private void updateInput(EnumFacing side) {
		int i = side.ordinal();
		int val = world.getRedstonePower(pos.offset(side), side);
		if (val != states[i]) {
			states[i] = val;
			if (dirty == 0) TickRegistry.instance.updates.add(this);
			dirty |= 1 << i;
		}
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {}

	@Override
	public void process() {
		for (int j = dirty, i = 0; j != 0; j >>>= 1, i++) {
			if ((j & 1) == 0) continue;
			IntConsumer c = callbacks[i];
			if (c != null) c.accept(states[i]);
		}
		dirty = 0;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (super.onActivated(player, hand, item, s, X, Y, Z)) return true;
		return cover.interact(this, player, hand, item, s, X, Y, Z);
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (cover.hit(this, player)) return;
		super.onClicked(player);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		NBTTagList list = new NBTTagList();
		for (MountedSignalPort port : ports) {
			NBTTagCompound tag = port.serializeNBT();
			tag.setByte("pin", (byte)port.pin);
			list.appendTag(tag);
		}
		nbt.setTag("ports", list);
		nbt.setByte("strong", strong);
		cover.writeNBT(nbt, "cover", mode == SYNC);
		NBTTagCompound tag = storeHooks();
		if (tag != null) nbt.setTag("hooks", tag);
		if (mode < SYNC) nbt.setIntArray("states", states);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		strong = nbt.getByte("strong");
		NBTTagList list = nbt.getTagList("ports", NBT.TAG_COMPOUND);
		ports = new MountedSignalPort[list.tagCount()];
		for (int i = 0; i < ports.length; i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			int pin = tag.getByte("pin");
			(ports[i] = createPort(pin)).deserializeNBT(tag);
		}
		cover.readNBT(nbt, "cover", mode == SYNC ? this : null);
		if (mode < SYNC) {
			int[] arr = nbt.getIntArray("states");
			System.arraycopy(arr, 0, states, 0, Math.min(arr.length, 12));
			if (arr.length < 12) Arrays.fill(states, arr.length, 12, 0);
		}
		loadHooks(nbt.getCompoundTag("hooks"));
		tesrComps = null;
		tesrBB = null;
		gui = null;
	}

	private MountedSignalPort createPort(int pin) {
		boolean in = pin < 6;
		MountedSignalPort port = new MountedSignalPort(this, pin, IntConsumer.class, in);
		EnumFacing side = EnumFacing.VALUES[pin % 6];
		Orientation o = Orientation.fromFacing(side);
		Vec3d p = o.rotate(new Vec3d(in ? -SIZE : SIZE, in ? -SIZE : SIZE, -0.375));
		port.setLocation((float)p.x + 0.5F, (float)p.y + 0.5F, (float)p.z + 0.5F, o.back);
		port.setName(port.isMaster ? "port.rs_ctr.rsR" : "port.rs_ctr.rsW");
		return port;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getModuleState(int m) {
		if (m == 7) return (T)getBMRComponents();
		if (m == 6) return cover.module();
		int i;
		if ((strong >> m & 1) != 0) i = 3;
		else {
			i = -1;
			if (getSignalPort(m) != null) i++;
			if (getSignalPort(m + 6) != null) i+=2;
		}
		return (T)Byte.valueOf((byte)i);
	}

	@Override
	public boolean isModulePresent(int m) {
		if (m == 7) return false;
		if (m == 6) return cover.state != null;
		return getSignalPort(m) != null || getSignalPort(m + 6) != null;
	}

	public boolean addPort(EnumFacing side, int type) {
		int i = side.ordinal();
		MountedSignalPort port;
		switch(type) {
		case 0:
			if (getSignalPort(i) != null || (strong >> i & 1) != 0) return false;
			port = createPort(i);
			break;
		case 1:
			if (getSignalPort(side.ordinal() + 6) != null) {
				if ((strong >> i & 1) != 0) return false;
				strong |= 1 << i;
				markDirty(REDRAW);
				return true;
			}
			port = createPort(i + 6);
			break;
		default: return false;
		}
		ports = Arrays.copyOf(ports, ports.length + 1);
		ports[ports.length - 1] = port;
		if (!unloaded && !world.isRemote) {
			port.onLoad();
			onPortModified(port, E_HOOK_ADD);
		}
		return true;
	}

	public boolean breakPort(int side, EntityPlayer player, boolean harvest) {
		if (side == 6) return cover.hit(this, player);
		boolean hasRem = false;
		int in = -1, out = -1;
		for (int i = 0; i < ports.length; i++)
			if (ports[i].pin == side) in = i;
			else if (ports[i].pin == side + 6) out = i;
			else hasRem = true;
		if (!hasRem) return false;
		if (out >= 0) {
			MountedSignalPort port = ports[out];
			if (harvest) ItemFluidUtil.dropStack(new ItemStack(Objects.rs_port, 1 + (strong >> side & 1), 1), world, pos);
			port.setConnector(null, player);
			port.onUnload();
			strong &= ~(1 << side);
			ports = ArrayUtils.remove(ports, out); //index shift doesn't affect in because it's always in < out. 
		}
		if (in >= 0) {
			MountedSignalPort port = ports[in];
			if (harvest) ItemFluidUtil.dropStack(new ItemStack(Objects.rs_port, 1, 0), world, pos);
			port.setConnector(null, player);
			port.onUnload();
			ports = ArrayUtils.remove(ports, in);
		}
		onPortModified(null, E_HOOK_REM);
		return true;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		neighborBlockChange(blockType, pos);
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		int in = 0, out = 0;
		for (MountedSignalPort port : ports) {
			if (port.isMaster) in++;
			else if ((strong >> (port.pin - 6) & 1) != 0) out+=2;
			else out++;
		}
		ArrayList<ItemStack> list = new ArrayList<>();
		if (in > 0) list.add(new ItemStack(Objects.rs_port, in, 0));
		if (out > 0) list.add(new ItemStack(Objects.rs_port, out, 1));
		if (cover.stack != null) list.add(cover.stack);
		return list;
	}

	@Override
	protected void setupData() {
		if (world.isRemote) return;
		//delay port registration until we have save block access
		TickRegistry.instance.updates.add(()-> {
			if (unloaded) return;
			for (MountedSignalPort port : ports)
				port.onLoad();
		});
	}

	@Override
	protected void clearData() {
		super.clearData();
		dirty = 0; //cancel scheduled updates
	}

	class RSOut implements IntConsumer {

		final BlockPos target;
		final EnumFacing side;
		final int id;

		RSOut(int id) {
			this.id = id;
			this.side = EnumFacing.VALUES[(id - 6)^1];
			this.target = pos.offset(side, -1);
		}

		@Override
		public void accept(int value) {
			if (value != states[id]) {
				states[id] = value;
				world.neighborChanged(target, blockType, pos);
				if ((strong >> (id - 6) & 1) != 0) world.notifyNeighborsOfStateExcept(target, blockType, side);
			}
		}

	}

}
