package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.wire.IWiredConnector;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class WireBranch extends Plug implements IWiredConnector, ITESRenderComp, IBlockRenderComp {

	public final WireType type;
	protected Port subPort;
	protected BlockPos linkPos;
	protected int linkPin;
	protected Vec3d line;
	protected int count;
	private float[] vertices; // render cache
	private int light1 = -1;

	public WireBranch(WireType type) {
		this.type = type;
	}

	public WireBranch subPort(Port subPort) {
		this.subPort = subPort;
		return this;
	}

	@Override
	protected String id() {
		return type.wiredId;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		if(subPort != null) nbt.merge(subPort.serializeNBT()); 
		if(linkPos != null) {
			nbt.setLong("pos", linkPos.toLong());
			nbt.setInteger("pin", linkPin);
			nbt.setFloat("dx", (float)line.x);
			nbt.setFloat("dy", (float)line.y);
			nbt.setFloat("dz", (float)line.z);
			nbt.setByte("count", (byte)count);
		}
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		if(subPort != null) subPort.deserializeNBT(nbt);
		if(nbt.hasKey("pos", NBT.TAG_LONG)) {
			line = new Vec3d(nbt.getFloat("dx"), nbt.getFloat("dy"), nbt.getFloat("dz"));
			linkPos = BlockPos.fromLong(nbt.getLong("pos"));
			linkPin = nbt.getInteger("pin");
			count = nbt.getByte("count") & 0xff;
		} else {
			line = null;
			linkPos = null;
			linkPin = 0;
			count = 0;
		}
	}

	@Override
	public void onRemoved(MountedPort port, EntityPlayer player) {
		if(linkPos == null && subPort != null) return;
		super.onRemoved(port, player);
		IWiredConnector.super.onRemoved(port, player);
		(subPort != null ? subPort : port).disconnect();
	}

	@Override
	public void onLoad(MountedPort port) {
		setPort(port);
		if(subPort != null) subPort.onLoad();
	}

	@Override
	public void onUnload() {
		if(subPort != null) subPort.onUnload();
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(type.wireItem, count);
	}

	@Override
	public void onWireRemoved(MountedPort host, MountedPort link, EntityPlayer player) {
		if(subPort != null) {
			subPort.disconnect();
			linkPos = null;
			line = null;
			host.owner.onPortModified(host, IPortProvider.E_CON_UPDATE);
		} else IWiredConnector.super.onWireRemoved(host, link, player);
	}

	@Override
	public void onLinkMove(MountedPort host, MountedPort link) {
		line = IWiredConnector.getPath(host, link).scale(.5);
		host.owner.onPortModified(host, IPortProvider.E_CON_UPDATE);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if(line == null) return;
		if(vertices == null) vertices = WireRenderer.createLine(port, line);
		if(light1 < 0) light1 = port.getWorld().getCombinedLight(
			port.getPos().add(line.x + port.pos.x, line.y + port.pos.y, line.z + port.pos.z), 0
		);
		WireRenderer.drawLine(buffer, vertices, (float)x, (float)y, (float)z, light, light1, type.color);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		if(line == null) return null;
		Vec3d p = new Vec3d(pos).add(port.pos);
		return new AxisAlignedBB(p, p.add(line));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		this.light1 = -1;
	}

	@Override
	public boolean isCompatible(Class<?> type) {
		return type == this.type.clazz;
	}

	@Override
	public Port getLinkPort(MountedPort from) {
		return linkPos != null ? IPortProvider.getPort(from.getWorld(), linkPos, linkPin) : null;
	}

	@Override
	public boolean isLinked(MountedPort to) {
		return linkPos != null && to.pin == linkPin && to.getPos().equals(linkPos);
	}

	//convenience implementations:

	public boolean addLink(MountedPort link, Vec3d line, EntityPlayer player) {
		if (linkPos != null) return false;
		linkPos = link.getPos();
		linkPin = link.pin;
		this.line = line;
		link.connect(subPort);
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
		return true;
	}

	public Port getLinkedWith(MountedPort link) {
		return isLinked(link) ? subPort : null;
	}

	public Port getPort(int pin) {
		return subPort;
	}

	public Object getPortCallback(int pin) {
		return null;
	}

	public void onPortModified(Port port, int event) {
		this.port.owner.onPortModified(this.port, IPortProvider.E_CON_UPDATE);
	}

}
