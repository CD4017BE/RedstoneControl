package cd4017be.rs_ctr.port;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IIntegratedConnector;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.wire.WireLine;
import cd4017be.api.rs_ctr.wire.WireLine.WireLoopException;
import cd4017be.api.rs_ctr.wire.WiredConnector;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public abstract class SplitPlug extends Connector implements IIntegratedConnector, ITagableConnector, ITESRenderComp, IBlockRenderComp {

	public static int MAX_LINK_COUNT = 8;

	public final Port inPort;
	Port[] ports = new Port[0];
	protected WireBranch[] wires = new WireBranch[0];

	public SplitPlug(MountedPort port) {
		super(port);
		this.inPort = new Port(this, 0, type().clazz, false);
	}

	protected abstract WireType type();

	@Override
	protected String id() {
		return type().splitId;
	}

	public int addLinks(int n) {
		int l = ports.length;
		n = Math.min(n, MAX_LINK_COUNT - l);
		if (n <= 0) return 0;
		ports = Arrays.copyOf(ports, l + n);
		wires = Arrays.copyOf(wires, l + n);
		for (int i = l; i < ports.length; i++)
			ports[i] = new Port(this, i + 1, type().clazz, true);
		return n;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.merge(inPort.serializeNBT());
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < ports.length; i++) {
			NBTTagCompound tag = ports[i].serializeNBT();
			if (wires[i] != null)
				tag.setTag("wire", wires[i].serializeNBT());
			list.appendTag(tag);
		}
		nbt.setTag("ports", list);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		inPort.deserializeNBT(nbt);
		if (nbt.hasKey("links", NBT.TAG_LIST)) {
			deserializeOld(nbt);
			return;
		}
		NBTTagList list = nbt.getTagList("ports", NBT.TAG_COMPOUND);
		ports = new Port[list.tagCount()];
		wires = new WireBranch[list.tagCount()];
		for (int i = 0; i < ports.length; i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			(ports[i] = new Port(this, i + 1, type().clazz, true)).deserializeNBT(tag);
			if (tag.hasKey("wire", NBT.TAG_COMPOUND))
				(wires[i] = new WireBranch(port, type())).deserializeNBT(tag.getCompoundTag("wire"));
		}
	}

	@Deprecated
	private void deserializeOld(NBTTagCompound nbt) {
		NBTTagList list = nbt.getTagList("links", NBT.TAG_COMPOUND);
		ports = new Port[list.tagCount()];
		wires = new WireBranch[list.tagCount()];
		for (int i = 0; i < ports.length; i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			(ports[i] = new Port(this, i + 1, type().clazz, true)).deserializeNBT(tag);
			if(tag.hasKey("pos", NBT.TAG_LONG))
				(wires[i] = new WireBranch(port, type())).deserializeNBT(tag);
		}
	}

	@Override
	public WiredConnector getLinkedWith(WiredConnector link) {
		for (WireBranch con : wires)
			if (con != null && con.isLinked(link))
				return con;
		return null;
	}

	@Override
	public boolean addWire(WiredConnector con, EntityPlayer player, boolean sim) {
		if (!(con instanceof WireBranch)) return false;
		for (int i = 0; i < wires.length; i++)
			if (wires[i] == null) {
				if (!sim) wires[i] = (WireBranch)con;
				return true;
			}
		ItemStack stack = drop();
		if (wires.length >= MAX_LINK_COUNT || !(player.isCreative() || player.inventory.hasItemStack(stack))) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.split1"));
			return false;
		}
		if (sim) return true;
		if (!player.isCreative()) player.inventory.clearMatchingItems(stack.getItem(), stack.getMetadata(), 1, null);
		addLinks(1);
		return addWire(con, player, sim);
	}

	@Override
	public void removeWire(WiredConnector con, EntityPlayer player) {
		for (int i = 0; i < wires.length; i++)
			if (wires[i] == con) {
				wires[i] = null;
				con.onRemoved(player);
			}
	}

	@Override
	public Port getPort(WiredConnector con) {
		for (int i = 0; i < wires.length; i++)
			if (wires[i] == con)
				return ports[i];
		return null;
	}

	@Override
	public void onLoad() {
		inPort.onLoad();
		for (Port port : ports) port.onLoad();
		for (WireBranch con : wires)
			if (con != null) con.onLoad();
		if (inPort.getLink() == 0) inPort.connect(port);
	}

	@Override
	public void onUnload() {
		inPort.onUnload();
		for (Port port : ports) port.onUnload();
		for (WireBranch con : wires)
			if (con != null) con.onUnload();
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		inPort.disconnect();
		for (int i = 0; i < wires.length; i++) {
			WireBranch con = wires[i];
			if (con == null) continue;
			wires[i] = null;
			con.onRemoved(player);
		}
		dropItem(drop(), player);
	}

	@Override
	public void onPortMove() {
		for (WireBranch con : wires)
			if (con != null)
				con.onPortMove();
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		int n = 0;
		for (WireBranch b : wires)
			if (b != null) n++;
		return "\n" + n + " / " + wires.length;
	}

	@Override
	public void setTag(String tag) {
		for (WireBranch b : wires) {
			if (b == null || Objects.equals(tag, b.getTag())) continue;
			try {
				new WireLine(b).forEach((con) -> con.setTag(tag));
			} catch(WireLoopException e) {}
		}
	}

	@Override
	public String getTag() {
		String s;
		for (WireBranch b : wires)
			if (b != null && (s = b.getTag()) != null)
				return s;
		return null;
	}

	@Override
	public Port getPort(int pin) {
		return pin == 0 ? inPort : ports[pin - 1];
	}

	@Override
	public Object getPortCallback(int pin) {
		return this;
	}

	@Override
	public void onPortModified(Port port, int event) {
		this.port.owner.onPortModified(this.port, E_CON_UPDATE);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		for (WireBranch b : wires)
			if (b != null)
				b.render(world, pos, x, y, z, light, buffer);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		AxisAlignedBB box = null;
		for (WireBranch b : wires) 
			if (b != null) {
				AxisAlignedBB box1 = b.getRenderBB(world, pos);
				if (box1 != null)
					box = box == null ? box1 : box.union(box1);
			}
		return box;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		for (WireBranch b : wires)
			if (b != null)
				b.render(quads);
		PortRenderer.PORT_RENDER.drawModel(
			quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z,
			Orientation.fromFacing(port.face), type().wireModel()
		);
	}

	protected abstract ItemStack drop();

}
