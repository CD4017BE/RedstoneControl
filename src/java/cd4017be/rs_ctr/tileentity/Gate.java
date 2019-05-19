package cd4017be.rs_ctr.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.ISelfAwareTile;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.rs_ctr.api.interact.IInteractiveDevice;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import cd4017be.rs_ctr.api.wire.IHookAttachable;
import cd4017be.rs_ctr.api.wire.RelayPort;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Template implementation of {@link ISignalIO}
 * @author CD4017BE
 */
public abstract class Gate extends BaseTileEntity implements IHookAttachable, IInteractiveTile, ISelfAwareTile, IInteractiveDevice, IModularTile {

	protected MountedSignalPort[] ports;
	protected IInteractiveComponent[] gui;
	protected Int2ObjectOpenHashMap<RelayPort> hooks = new Int2ObjectOpenHashMap<>();
	protected Orientation o = Orientation.N;

	@Override
	public SignalPort getSignalPort(int pin) {
		SignalPort port = hooks.get(pin);
		if (port != null)
			return port;
		if (pin < ports.length && (port = ports[pin]).pin == pin)
			return port;
		return IHookAttachable.super.getSignalPort(pin);
	}

	@Override
	public Orientation getOrientation() {
		return o;
	}

	@Override
	public Int2ObjectMap<RelayPort> getHookPins() {
		return hooks;
	}

	@Override
	public IInteractiveComponent[] getComponents() {
		if (gui == null) {
			List<IInteractiveComponent> list = new ArrayList<>();
			Collections.addAll(list, ports);
			for (RelayPort port : hooks.values())
				if (port.isSource)
					list.add(port);
			initGuiComps(list);
			gui = list.toArray(new IInteractiveComponent[list.size()]);
		}
		return gui;
	}

	protected void initGuiComps(List<IInteractiveComponent> list) {}

	@Override
	public SignalPort[] getSignalPorts() {
		return ports;
	}

	@Override
	public void onPortModified(SignalPort port, int event) {
		int mode;
		if ((event & (E_HOOK_ADD | E_HOOK_REM)) != 0) {
			gui = null;
			mode = REDRAW;
		} else if ((event & E_CON_UPDATE) != 0 && ((event & E_CON_REM) != 0 || ((MountedSignalPort)port).getConnector() instanceof IBlockRenderComp)) {
			mode = REDRAW;
		} else mode = SYNC;
		markDirty(mode);
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		Triple<IInteractiveComponent, Vec3d, EnumFacing> r = rayTrace(player, 1);
		return r != null && (world.isRemote || r.getLeft().onInteract(player, false, r.getRight(), r.getMiddle()));
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (world.isRemote) return;
		Triple<IInteractiveComponent, Vec3d, EnumFacing> r = rayTrace(player, 1);
		if (r != null) r.getLeft().onInteract(player, true, r.getRight(), r.getMiddle());
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		NBTTagList list = new NBTTagList();
		for (MountedSignalPort port : ports)
			list.appendTag(port.serializeNBT());
		nbt.setTag("ports", list);
		NBTTagCompound ctag = storeHooks();
		if (ctag != null) nbt.setTag("hooks", ctag);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		NBTTagList list = nbt.getTagList("ports", NBT.TAG_COMPOUND);
		for (int i = 0; i < ports.length; i++)
			ports[i].deserializeNBT(list.getCompoundTagAt(i));
		loadHooks(nbt.getCompoundTag("hooks"));
		tesrComps = null;
		tesrBB = null;
		gui = null;
	}

	@Override
	protected void setupData() {
		if (world.isRemote) return;
		for (MountedSignalPort port : ports)
			port.onLoad();
	}

	@Override
	protected void clearData() {
		if (world.isRemote) return;
		for (MountedSignalPort port : ports)
			port.onUnload();
	}

	@Override
	public void invalidate() {
		if (!world.isRemote)
			for (MountedSignalPort port : ports)
				port.disconnect();
		super.invalidate();
	}

	@Override
	public void breakBlock() {
		for (MountedSignalPort port : ports)
			port.setConnector(null, null);
	}

	@Override
	public ArrayList<IBlockRenderComp> getBMRComponents() {
		ArrayList<IBlockRenderComp> res = new ArrayList<>();
		for (IInteractiveComponent c : getComponents()) {
			if (c instanceof IBlockRenderComp)
				res.add((IBlockRenderComp) c);
			if (c instanceof MountedSignalPort)
				((MountedSignalPort)c).addRenderComps(res, IBlockRenderComp.class);
		}
		return res;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getModuleState(int m) {
		return (T)getBMRComponents();
	}

	@Override
	public boolean isModulePresent(int m) {
		return false;
	}

	/** cached tesr render components */
	protected ArrayList<ITESRenderComp> tesrComps = null;
	/** cached tesr render range */
	protected AxisAlignedBB tesrBB = null;

	@Override
	public ArrayList<ITESRenderComp> getTESRComponents() {
		if (tesrComps == null) {
			tesrComps = new ArrayList<>();
			for (IInteractiveComponent c : getComponents()) {
				if (c instanceof ITESRenderComp)
					tesrComps.add((ITESRenderComp) c);
				if (c instanceof MountedSignalPort)
					((MountedSignalPort)c).addRenderComps(tesrComps, ITESRenderComp.class);
			}
		}
		return tesrComps;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if (tesrBB == null) {
			tesrBB = super.getRenderBoundingBox();
			for (ITESRenderComp c : getTESRComponents()) {
				AxisAlignedBB bb = c.getRenderBB(world, pos);
				if (bb != null)
					tesrBB = tesrBB.union(bb);
			}
		}
		return tesrBB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasFastRenderer() {
		return !HybridFastTESR.isAimedAt(this);
	}

	@Override
	public void updateContainingBlockInfo() {
		super.updateContainingBlockInfo();
		o = super.getOrientation();
		for (RelayPort port : hooks.values())
			port.orient(o);
		gui = null;
	}

}
