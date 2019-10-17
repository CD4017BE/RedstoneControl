package cd4017be.rs_ctr.tileentity;

import static cd4017be.api.rs_ctr.port.MountedPort.SIZE;
import static cd4017be.rs_ctr.ClientProxy.t_sockets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.render.Util;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.Utils;
import static cd4017be.rs_ctr.Objects.rs_port;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.wrapper.EmptyHandler;
import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/**
 * @author CD4017BE
 *
 */
public class RedstonePort extends Gate implements IRedstoneTile, INeighborAwareTile, IUpdatable, IModularTile, ITilePlaceHarvest, IBlockRenderComp {

	private static boolean RECURSION;

	final SignalHandler[] callbacks = new SignalHandler[6];
	final BlockReference[] mirrors = new BlockReference[6];
	/**0-5: input, 6-11: output */
	final int[] states = new int[12];
	byte strong, dirty;

	{ports = new MountedPort[0];} //keep array sorted!

	@Override
	public Port getPort(int pin) {
		Port port = super.getPort(pin);
		if (port != null) return port;
		int i = Arrays.binarySearch(ports, pin);
		return i >= 0 ? ports[i] : null;
	}

	@Override
	public Object getPortCallback(int pin) {
		if (pin < 12) return new RSOut(pin);
		int i = pin - 18;
		return (BlockHandler)(block) -> mirrors[i] = block;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if (pin < 6) {
			SignalHandler c = callback instanceof SignalHandler ? (SignalHandler)callback : null;
			callbacks[pin] = c;
			if (c != null) c.updateSignal(states[pin]);
		} else if (callback instanceof BlockHandler) {
			EnumFacing side = EnumFacing.VALUES[(pin - 12)^1];
			((BlockHandler)callback).updateBlock(new BlockReference(world, pos.offset(side, -1), side));
		}
	}

	@Override
	protected void resetPin(int pin) {
		if (pin < 12) new RSOut(pin).updateSignal(0);
		else mirrors[pin - 18] = null;
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		int i = side.ordinal();
		return !strong || (this.strong >> i & 1) != 0 ? states[i+6] : 0;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		int i = side.ordinal();
		return getPort(i) != null || getPort(i + 6) != null;
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
			SignalHandler c = callbacks[i];
			if (c != null) c.updateSignal(states[i]);
		}
		dirty = 0;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		if (cap != ITEM_HANDLER_CAPABILITY && cap != FLUID_HANDLER_CAPABILITY) return false;
		int i = facing.ordinal();
		return mirrors[i] != null || Arrays.binarySearch(ports, i + 18) >= 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap != ITEM_HANDLER_CAPABILITY && cap != FLUID_HANDLER_CAPABILITY) return null;
		int i = facing.ordinal();
		BlockReference block = mirrors[i];
		if (block != null && !RECURSION) {
			try {
				RECURSION = true;
				if (block.isLoaded()) {
					T obj = block.getCapability(cap);
					if (obj != null) return obj;
				}
			} finally { RECURSION = false; }
		} else if (Arrays.binarySearch(ports, i + 18) < 0) return null;
		return (T)(cap == ITEM_HANDLER_CAPABILITY ? EmptyHandler.INSTANCE : EmptyFluidHandler.INSTANCE);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		NBTTagList list = new NBTTagList();
		for (MountedPort port : ports) {
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
		ports = new MountedPort[list.tagCount()];
		for (int i = 0; i < ports.length; i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			int pin = tag.getByte("pin");
			(ports[i] = createPort(pin)).deserializeNBT(tag);
		}
		Arrays.sort(ports);
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

	private MountedPort createPort(int pin) {
		boolean br = pin >= 12;
		boolean in = pin % 12 < 6;
		MountedPort port = new MountedPort(this, pin, br ? BlockHandler.class : SignalHandler.class, in);
		EnumFacing side = EnumFacing.VALUES[pin % 6];
		Orientation o = Orientation.fromFacing(side);
		Vec3d p = o.rotate(new Vec3d(in ? -SIZE : SIZE, in^br ? -SIZE : SIZE, -0.375));
		port.setLocation((float)p.x + 0.5F, (float)p.y + 0.5F, (float)p.z + 0.5F, o.back);
		port.setName(br ? in ? "port.rs_ctr.brR" : "port.rs_ctr.brW" : in ? "port.rs_ctr.rsR" : "port.rs_ctr.rsW");
		return port;
	}

	@Override
	public ArrayList<IBlockRenderComp> getBMRComponents() {
		ArrayList<IBlockRenderComp> list = super.getBMRComponents();
		list.add(this);
		return list;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		for (MountedPort port : ports) {
			Orientation o = Orientation.fromFacing(port.face);
			Vec3d dx = o.X.scale(MountedPort.SIZE * -2.), dy = o.Y.scale(MountedPort.SIZE * 2.), p = o.rotate(new Vec3d(MountedPort.SIZE, -MountedPort.SIZE, -0.005));
			int i = port.isMaster ? 4 : 0, j = port.type == SignalHandler.class ? 0 : port.type == BlockHandler.class ? 4 : port.type == EnergyHandler.class ? 8 : 12;
			quads.add(new BakedQuad(Util.texturedRect(port.pos.add(p), dx, dy, Util.getUV(t_sockets, i, j), Util.getUV(t_sockets, i + 4, j + 4), -1, 0), -1, o.front, t_sockets, true, DefaultVertexFormats.BLOCK));
		}
		for (int i = 0; i < 6; i++)
			if ((strong >> i & 1) != 0) {
				Orientation o = Orientation.fromFacing(EnumFacing.VALUES[i]);
				Vec3d dx = o.X.scale(MountedPort.SIZE * 2.), dy = o.Y.scale(MountedPort.SIZE * 2.), p = o.rotate(new Vec3d(-.125 - MountedPort.SIZE , -.125 - MountedPort.SIZE , -.37));
				quads.add(new BakedQuad(Util.texturedRect(p.addVector(.5, .5, .5), dx, dy, Util.getUV(t_sockets, 0, 12), Util.getUV(t_sockets, 4, 16), -1, 0), -1, o.back, t_sockets, true, DefaultVertexFormats.BLOCK));
			}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getModuleState(int m) {
		if (m == 7) return (T)getBMRComponents();
		if (m == 6) return cover.module();
		return (T)Byte.valueOf(isModulePresent(m) ? (byte)0 : (byte)-1);
	}

	@Override
	public boolean isModulePresent(int m) {
		if (m == 7) return false;
		if (m == 6) return cover.state != null;
		return Arrays.binarySearch(ports, m) >= 0 ||
		Arrays.binarySearch(ports, m + 6) >= 0 ||
		Arrays.binarySearch(ports, m + 12) >= 0 ||
		Arrays.binarySearch(ports, m + 18) >= 0;
	}

	public boolean addPort(EnumFacing side, int type) {
		int i = side.ordinal(), k = i + 6 * type;
		int j = Arrays.binarySearch(ports, k);
		switch(type) {
		case 0:
			if (j >= 0 || (strong >> i & 1) != 0) return false;
			break;
		case 1:
			if (j >= 0) {
				if ((strong >> i & 1) != 0) return false;
				strong |= 1 << i;
				markDirty(REDRAW);
				return true;
			}
			break;
		case 2:
		case 3:
			if (j < 0) break;
		default: return false;
		}
		MountedPort port = createPort(k);
		ports = ArrayUtils.add(ports, -1 - j, port);
		if (!unloaded && !world.isRemote) {
			port.onLoad();
			onPortModified(port, E_HOOK_ADD);
		}
		return true;
	}

	public boolean breakPort(int side, EntityPlayer player, boolean harvest) {
		if (side <= -2) return cover.hit(this, player);
		int i;
		if ((i = Arrays.binarySearch(ports, side + 18)) >= 0) {
			if (harvest) ItemFluidUtil.dropStack(new ItemStack(rs_port, 1, 3), world, pos);
			remPort(i, player);
		}
		if ((i = Arrays.binarySearch(ports, side + 12)) >= 0) {
			if (harvest) ItemFluidUtil.dropStack(new ItemStack(rs_port, 1, 2), world, pos);
			remPort(i, player);
		}
		if ((i = Arrays.binarySearch(ports, side + 6)) >= 0) {
			if (harvest) ItemFluidUtil.dropStack(new ItemStack(rs_port, 1 + (strong >> side & 1), 1), world, pos);
			remPort(i, player);
			strong &= ~(1 << side);
		}
		if ((i = Arrays.binarySearch(ports, side)) >= 0) {
			if (harvest) ItemFluidUtil.dropStack(new ItemStack(rs_port, 1, 0), world, pos);
			remPort(i, player);
		}
		onPortModified(null, E_HOOK_REM);
		return ports.length > 0;
	}

	private void remPort(int i, EntityPlayer player) {
		MountedPort port = ports[i];
		port.setConnector(null, player);
		port.onUnload();
		ports = ArrayUtils.remove(ports, i);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		neighborBlockChange(getBlockType(), pos);
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		int in = 0, out = 0, br = 0, mirr = 0;
		for (MountedPort port : ports)
			if (port.type == BlockHandler.class)
				if (port.isMaster) br++;
				else mirr++;
			else if (port.isMaster) in++;
			else if ((strong >> (port.pin - 6) & 1) != 0) out+=2;
			else out++;
		ArrayList<ItemStack> list = new ArrayList<>();
		if (in > 0) list.add(new ItemStack(rs_port, in, 0));
		if (out > 0) list.add(new ItemStack(rs_port, out, 1));
		if (br > 0) list.add(new ItemStack(rs_port, br, 2));
		if (mirr > 0) list.add(new ItemStack(rs_port, mirr, 2));
		return list;
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		dirty = 0; //cancel scheduled updates
	}

	class RSOut implements SignalHandler {

		final BlockPos target;
		final EnumFacing side;
		final int id;

		RSOut(int id) {
			this.id = id;
			this.side = EnumFacing.VALUES[(id - 6)^1];
			this.target = pos.offset(side, -1);
		}

		@Override
		public void updateSignal(int value) {
			if (value != states[id]) {
				states[id] = value;
				world.neighborChanged(target, getBlockType(), pos);
				if ((strong >> (id - 6) & 1) != 0) world.notifyNeighborsOfStateExcept(target, blockType, side);
			}
		}

	}

}
