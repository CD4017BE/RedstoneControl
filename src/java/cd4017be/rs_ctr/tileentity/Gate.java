package cd4017be.rs_ctr.tileentity;

import java.util.ArrayList;
import org.apache.commons.lang3.tuple.Triple;

import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.ISelfAwareTile;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.rs_ctr.api.interact.IInteractiveDevice;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
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
public abstract class Gate extends BaseTileEntity implements ISignalIO, IInteractiveTile, ISelfAwareTile, IInteractiveDevice, IModularTile {

	protected MountedSignalPort[] ports;

	@Override
	public IInteractiveComponent[] getComponents() {
		return ports;
	}

	@Override
	public SignalPort[] getSignalPorts() {
		return ports;
	}

	@Override
	public void onPortModified(SignalPort port, int event) {
		markDirty((event & E_CON_UPDATE) != 0 && ((event & E_CON_REM) != 0 || port instanceof IBlockRenderComp || ((MountedSignalPort)port).getConnector() instanceof IBlockRenderComp) ? REDRAW : SYNC);
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
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		NBTTagList list = nbt.getTagList("ports", NBT.TAG_COMPOUND);
		for (int i = 0; i < ports.length; i++)
			ports[i].deserializeNBT(list.getCompoundTagAt(i));
		tesrComps = null;
		tesrBB = null;
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

}
