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
import cd4017be.api.rs_ctr.wire.IWiredConnector;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import cd4017be.rs_ctr.render.WireRenderer;
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

	protected Branch[] links;
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
		links = l == 0 ? new Branch[n] : Arrays.copyOf(links, l + n);
		for (int i = l; i < links.length; i++)
			links[i] = new Branch(i);
		return n;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		if (tag != null) nbt.setString("tag", tag);
		NBTTagList list = new NBTTagList();
		for (Branch b : links)
			list.appendTag(b.serializeNBT());
		nbt.setTag("links", list);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		tag = nbt.hasKey("tag", NBT.TAG_STRING) ? nbt.getString("tag") : null;
		NBTTagList list = nbt.getTagList("links", NBT.TAG_COMPOUND);
		links = new Branch[list.tagCount()];
		for (int i = 0; i < links.length; i++)
			(links[i] = new Branch(i)).deserializeNBT(list.getCompoundTagAt(i));
	}

	@Override
	public void onLoad(MountedPort port) {
		super.onLoad(port);
		port.owner.setPortCallback(port.pin, this);
		for (Branch b : links)
			b.port.onLoad();
	}

	@Override
	public void onUnload() {
		if (port == null) return;
		port.owner.setPortCallback(port.pin, null);
		for (Branch b : links)
			b.port.onUnload();
		this.port = null;
	}

	@Override
	public void onRemoved(MountedPort port, EntityPlayer player) {
		super.onRemoved(port, player);
		for (Branch b : links)
			b.onRemoved(port, player);
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		StringBuilder sb = new StringBuilder();
		if (tag != null) sb.append("\n\u00a7e").append(tag);
		sb.append('\n');
		int n = 0;
		for (Branch b : links)
			if (b.port.getLink() != 0) n++;
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
		for (Branch b : links)
			if (b.isLinked(link))
				return b.port;
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
		for (Branch b : links) {
			if (!b.isLinked(link)) continue;
			b.onWireRemoved(host, link, player);
			return;
		}
	}

	@Override
	public void onPortMove(MountedPort port) {
		for (Branch b : links)
			b.onPortMove(port);
	}

	@Override
	public void onLinkMove(MountedPort host, MountedPort link) {
		for (Branch b : links) {
			if (!b.isLinked(link)) continue;
			b.onLinkMove(host, link);
			return;
		}
	}

	@Override
	public boolean addLink(MountedPort link, Vec3d line, EntityPlayer player) {
		for (Branch b : links) {
			if (b.linkPos != null) continue;
			b.linkPos = link.getPos();
			b.linkPin = link.pin;
			b.line = line;
			link.connect(b.port);
			port.owner.onPortModified(port, E_CON_UPDATE);
			return true;
		}
		ItemStack stack = drop();
		if (!(player.isCreative() || player.inventory.hasItemStack(stack)) || addLinks(1) != 1) return false;
		if (!player.isCreative()) 
			player.inventory.clearMatchingItems(stack.getItem(), stack.getMetadata(), 1, null);
		return addLink(link, line, player);
	}

	@Override
	public Port getPort(int pin) {
		return links[pin].port;
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
		for (Branch b : links) {
			if (b.line == null) continue;
			if (b.vertices == null) b.vertices = WireRenderer.createLine(port, b.line);
			if (b.light1 < 0) b.light1 = port.getWorld().getCombinedLight(port.getPos().add(b.line.x + port.pos.x, b.line.y + port.pos.y, b.line.z + port.pos.z), 0);
			WireRenderer.drawLine(buffer, b.vertices, (float)x, (float)y, (float)z, light, b.light1, type().color);
		}
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		Vec3d p = new Vec3d(pos).add(port.pos);
		AxisAlignedBB box = null;
		for (Branch b : links) {
			if (b.line == null) continue;
			AxisAlignedBB box1 = new AxisAlignedBB(p, p.add(b.line));
			box = box == null ? box1 : box.union(box1);
		}
		return box;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		for (Branch b : links)
			b.light1 = -1;
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), type().model);
	}


	public class Branch implements IWiredConnector {

		private BlockPos linkPos;
		private int linkPin;
		private Vec3d line;
		private final Port port;
		private float[] vertices; //render cache
		private int light1 = -1;

		public Branch(int id) {
			this.port = new Port(SplitPlug.this, id, type().clazz, true);
		}
	
		@Override
		public void onRemoved(MountedPort port, EntityPlayer player) {
			if (linkPos == null) return;
			IWiredConnector.super.onRemoved(port, player);
			this.port.disconnect();
		}

		@Override
		public NBTTagCompound serializeNBT() {
			NBTTagCompound nbt = port.serializeNBT();
			if (linkPos != null) {
				nbt.setLong("pos", linkPos.toLong());
				nbt.setInteger("pin", linkPin);
				nbt.setFloat("dx", (float)line.x);
				nbt.setFloat("dy", (float)line.y);
				nbt.setFloat("dz", (float)line.z);
			}
			return nbt;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			port.deserializeNBT(nbt);
			if (nbt.hasKey("pos", NBT.TAG_LONG)) {
				line = new Vec3d(nbt.getFloat("dx"), nbt.getFloat("dy"), nbt.getFloat("dz"));
				linkPos = BlockPos.fromLong(nbt.getLong("pos"));
				linkPin = nbt.getInteger("pin");
			} else {
				line = null;
				linkPos = null;
				linkPin = 0;
			}
		}

		@Override
		public Port getLinkPort(MountedPort from) {
			return linkPos != null ? IPortProvider.getPort(from.getWorld(), linkPos, linkPin) : null;
		}

		@Override
		public boolean isLinked(MountedPort to) {
			return linkPos != null && to.pin == linkPin && to.getPos().equals(linkPos);
		}

		@Override
		public boolean isCompatible(Class<?> type) {
			return type == type().clazz;
		}

		@Override
		public void onWireRemoved(MountedPort host, MountedPort link, EntityPlayer player) {
			port.disconnect();
			linkPos = null;
			line = null;
			host.owner.onPortModified(host, E_CON_UPDATE);
		}

		@Override
		public void onLinkMove(MountedPort host, MountedPort link) {
			line = IWiredConnector.getPath(host, link).scale(.5);
			host.owner.onPortModified(host, E_CON_UPDATE);
		}

	}

}
