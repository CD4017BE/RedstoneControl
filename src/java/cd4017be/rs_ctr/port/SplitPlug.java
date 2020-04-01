package cd4017be.rs_ctr.port;

import java.util.Arrays;
import java.util.List;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.IIntegratedConnector;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public abstract class SplitPlug extends Plug implements IIntegratedConnector, ITagableConnector, ITESRenderComp, IBlockRenderComp {

	public static int MAX_LINK_COUNT = 8;

	protected WireBranch[] links;
	private String tag;

	protected abstract WireType type();

	@Override
	protected String id() {
		return type().splitId;
	}

	public int addLinks(int n) {
		int l = links == null ? 0 : links.length;
		n = Math.min(n, MAX_LINK_COUNT - l);
		if (n <= 0) return 0;
		links = l == 0 ? new WireBranch[n] : Arrays.copyOf(links, l + n);
		for (int i = l; i < links.length; i++) {
			WireBranch b = new WireBranch(type()).subPort(new Port(SplitPlug.this, i, type().clazz, true));
			b.setPort(port);
			links[i] = b;
		}
		return n;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		if (tag != null) nbt.setString("tag", tag);
		NBTTagList list = new NBTTagList();
		for (WireBranch b : links)
			list.appendTag(b.serializeNBT());
		nbt.setTag("links", list);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		tag = nbt.hasKey("tag", NBT.TAG_STRING) ? nbt.getString("tag") : null;
		NBTTagList list = nbt.getTagList("links", NBT.TAG_COMPOUND);
		links = new WireBranch[list.tagCount()];
		for (int i = 0; i < links.length; i++)
			(links[i] = new WireBranch(type()).subPort(new Port(SplitPlug.this, i, type().clazz, true)))
				.deserializeNBT(list.getCompoundTagAt(i));
	}

	@Override
	public void setPort(MountedPort port) {
		super.setPort(port);
		for (WireBranch b : links)
			b.setPort(port);
	}

	@Override
	public void onLoad(MountedPort port) {
		super.onLoad(port);
		port.owner.setPortCallback(port.pin, this);
		for (WireBranch b : links)
			b.onLoad(port);
	}

	@Override
	public void onUnload() {
		if (port == null) return;
		port.owner.setPortCallback(port.pin, null);
		for (WireBranch b : links)
			b.onUnload();
		this.port = null;
	}

	@Override
	public void onRemoved(MountedPort port, EntityPlayer player) {
		super.onRemoved(port, player);
		for (WireBranch b : links)
			b.onRemoved(port, player);
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		StringBuilder sb = new StringBuilder();
		if (tag != null) sb.append("\n\u00a7e").append(tag);
		sb.append('\n');
		int n = 0;
		for (WireBranch b : links)
			if (b.subPort.getLink() != 0) n++;
		return sb.append(n).append(" / ").append(links.length).toString();
	}

	@Override
	public void setTag(MountedPort port, String tag) {
		this.tag = tag;
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
	}

	@Override
	public String getTag() {
		return tag;
	}

	@Override
	public Port getLinkedWith(MountedPort link) {
		for (WireBranch b : links)
			if (b.isLinked(link))
				return b.subPort;
		return null;
	}

	@Override
	public Port getLinkPort(MountedPort from) {
		return null;
	}

	@Override
	public boolean isCompatible(Class<?> type) {
		return type == type().clazz;
	}

	@Override
	public void onWireRemoved(MountedPort host, MountedPort link, EntityPlayer player) {
		for (WireBranch b : links) {
			if (!b.isLinked(link)) continue;
			b.onWireRemoved(host, link, player);
			return;
		}
	}

	@Override
	public void onPortMove(MountedPort port) {
		for (WireBranch b : links)
			b.onPortMove(port);
	}

	@Override
	public void onLinkMove(MountedPort host, MountedPort link) {
		for (WireBranch b : links) {
			if (!b.isLinked(link)) continue;
			b.onLinkMove(host, link);
			return;
		}
	}

	@Override
	public boolean addLink(MountedPort link, Vec3d line, EntityPlayer player) {
		for (WireBranch b : links)
			if (b.addLink(link, line, player)) return true;
		ItemStack stack = drop();
		if (!(player.isCreative() || player.inventory.hasItemStack(stack)) || addLinks(1) != 1) return false;
		if (!player.isCreative()) 
			player.inventory.clearMatchingItems(stack.getItem(), stack.getMetadata(), 1, null);
		return addLink(link, line, player);
	}

	@Override
	public Port getPort(int pin) {
		return links[pin].subPort;
	}

	@Override
	public Object getPortCallback(int pin) {
		return null;
	}

	@Override
	public void onPortModified(Port port, int event) {
		this.port.owner.onPortModified(this.port, E_CON_UPDATE);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		for (WireBranch b : links)
			b.render(world, pos, x, y, z, light, buffer);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		AxisAlignedBB box = null;
		for (WireBranch b : links) {
			AxisAlignedBB box1 =b.getRenderBB(world, pos);
			if (box1 != null)
				box = box == null ? box1 : box.union(box1);
		}
		return box;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		for (WireBranch b : links)
			b.render(quads);
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), type().wireModel());
	}

}
