package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IIntegratedConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.wire.WiredConnector;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public abstract class LogicPlug extends Connector implements IIntegratedConnector, SignalHandler, IBlockRenderComp, ITESRenderComp {

	WireBranch wire;
	final Port inPort, outPort;
	SignalHandler out = SignalHandler.NOP;

	public LogicPlug(MountedPort port) {
		super(port);
		this.inPort = new Port(this, 0, SignalHandler.class, false);
		this.outPort = new Port(this, 1, SignalHandler.class, true);
	}

	@Override
	public Port getPort(int pin) {
		return pin == 0 ? inPort : outPort;
	}

	@Override
	public Object getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if (callback instanceof SignalHandler)
			(out = (SignalHandler)callback).updateSignal(getOutput());
		else out = SignalHandler.NOP;
	}

	protected abstract int getOutput();

	@Override
	public void onPortModified(Port port, int event) {
		this.port.owner.onPortModified(this.port, E_CON_UPDATE);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setTag("pi", inPort.serializeNBT());
		nbt.setTag("po", outPort.serializeNBT());
		if (wire != null) nbt.setTag("wire", wire.serializeNBT());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		inPort.deserializeNBT(nbt.getCompoundTag("pi"));
		outPort.deserializeNBT(nbt.getCompoundTag("po"));
		if (nbt.hasKey("wire", NBT.TAG_COMPOUND))
			(wire = new WireBranch(port, WireType.SIGNAL)).deserializeNBT(nbt.getCompoundTag("wire"));
		else wire = null;
	}

	@Override
	public WiredConnector getLinkedWith(WiredConnector link) {
		return wire != null && wire.isLinked(link) ? wire : null;
	}

	@Override
	public boolean addWire(WiredConnector con, EntityPlayer player, boolean sim) {
		if (!(con instanceof WireBranch)) return false;
		if (sim) return true;
		if (wire != null) removeWire(wire, player);
		wire = (WireBranch)con;
		return true;
	}

	@Override
	public void removeWire(WiredConnector con, EntityPlayer player) {
		if (con != wire) return;
		wire = null;
		con.onRemoved(player);
	}

	@Override
	public Port getPort(WiredConnector con) {
		return con == wire ? outPort : null;
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		if (wire != null) removeWire(wire, player);
		inPort.disconnect();
	}

	@Override
	public void onLoad() {
		inPort.onLoad();
		outPort.onLoad();
		if (wire != null) wire.onLoad();
	}

	@Override
	public void onUnload() {
		inPort.onUnload();
		outPort.onUnload();
		if (wire != null) wire.onUnload();
	}

	@Override
	public void onPortMove() {
		if (wire != null) wire.onPortMove();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (wire != null) wire.render(world, pos, x, y, z, light, buffer);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return wire != null ? wire.getRenderBB(world, pos) : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		if (wire != null) wire.render(quads);
	}

}
