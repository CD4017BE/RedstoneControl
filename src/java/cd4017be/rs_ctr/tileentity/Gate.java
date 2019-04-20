package cd4017be.rs_ctr.tileentity;

import org.apache.commons.lang3.tuple.Triple;

import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.ISelfAwareTile;
import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.api.interact.IInteractiveDevice;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
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
public abstract class Gate extends BaseTileEntity implements ISignalIO, IInteractiveTile, ISelfAwareTile, IInteractiveDevice {

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
		markDirty();
		markUpdate();
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
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		writePorts(nbt);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readPorts(pkt.getNbtCompound());
		renderBox = null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		readPorts(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		writePorts(nbt);
		return super.writeToNBT(nbt);
	}

	protected void writePorts(NBTTagCompound nbt) {
		NBTTagList list = new NBTTagList();
		for (MountedSignalPort port : ports)
			list.appendTag(port.serializeNBT());
		nbt.setTag("ports", list);
	}

	protected void readPorts(NBTTagCompound nbt) {
		NBTTagList list = nbt.getTagList("ports", NBT.TAG_COMPOUND);
		for (int i = 0; i < ports.length; i++)
			ports[i].deserializeNBT(list.getCompoundTagAt(i));
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

	protected AxisAlignedBB renderBox;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if (renderBox == null) {
			renderBox = super.getRenderBoundingBox();
			for (MountedSignalPort port : ports) {
				IConnector c = port.getConnector();
				if (c == null) continue;
				AxisAlignedBB box = c.renderSize(world, pos, port);
				if (box != null)
					renderBox = renderBox.union(box);
			}
		}
		return renderBox;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasFastRenderer() {
		return !HybridFastTESR.isAimedAt(this);
	}

}
