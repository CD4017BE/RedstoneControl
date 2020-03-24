package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.lib.util.DimPos;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class WirelessConnection extends Connector implements ITagableConnector, IBlockRenderComp {

	private final WireType type;
	private DimPos linkPos;
	private int linkPin;
	private boolean dropsItem;
	private String tag;

	public WirelessConnection(MountedPort port, WireType type) {
		super(port);
		this.type = type;
	}

	public WirelessConnection(MountedPort port, DimPos linkPos, int linkPin, boolean drop, WireType type) {
		this(port, type);
		this.linkPos = linkPos;
		this.linkPin = linkPin;
		this.dropsItem = drop;
	}

	@Override
	protected String id() {
		return type.wirelessId;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setLong("pos", linkPos.toLong());
		nbt.setInteger("dim", linkPos.dimId);
		nbt.setInteger("pin", linkPin);
		nbt.setBoolean("drop", dropsItem);
		if(tag != null) nbt.setString("tag", tag);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		linkPos = new DimPos(BlockPos.fromLong(nbt.getLong("pos")), nbt.getInteger("dim"));
		linkPin = nbt.getInteger("pin");
		dropsItem = nbt.getBoolean("drop");
		tag = nbt.hasKey("tag", NBT.TAG_STRING) ? nbt.getString("tag") : null;
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		try {
			return (tag != null ? "\n\u00a7e" + tag : super.displayInfo(port, linkID))
				+ "\n[" + linkPos.getX() + ", " + linkPos.getY() + ", " + linkPos.getZ() + "]\n"
				+ DimensionManager.getProviderType(linkPos.dimId).getName();
		} catch(IllegalArgumentException e) {
			return "\n" + e.getMessage();
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(
			quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z,
			Orientation.fromFacing(port.face), type.wirelessModel()
		);
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		Port p = IPortProvider.getPort(linkPos.getWorldServer(), linkPos, linkPin);
		if(p != null && p.getLink() == port.getLink() && p instanceof MountedPort) {
			Connector c = ((MountedPort)p).getConnector();
			if(c instanceof WirelessConnection) {
				WirelessConnection wc = (WirelessConnection)c;
				if(wc.dropsItem && !this.dropsItem) {
					this.dropsItem = true;
					wc.dropsItem = false;
				}
				((MountedPort)p).setConnector(null, player);
			}
		}
		if(dropsItem) dropItem(new ItemStack(type.wirelessItem), player);
		port.disconnect();
	}

	@Override
	public void setTag(String tag) {
		if(this.tag != null ? this.tag.equals(tag) : tag == null) return;
		this.tag = tag;
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
		Port p = IPortProvider.getPort(linkPos.getWorldServer(), linkPos, linkPin);
		if(p instanceof MountedPort) {
			Connector c = ((MountedPort)p).getConnector();
			if(c instanceof WirelessConnection) {
				((WirelessConnection)c).tag = tag;
				p.owner.onPortModified(p, IPortProvider.E_CON_UPDATE);
			}
		}
	}

	@Override
	public String getTag() {
		return tag;
	}

	@Override
	public void onLinkLoad(Port link) {
		if (!(link instanceof MountedPort)) return;
		Connector con = ((MountedPort)link).getConnector();
		if (!(con instanceof WirelessConnection)) return;
		WirelessConnection wc = (WirelessConnection)con;
		DimPos pos = new DimPos(wc.port.getPos(), wc.port.getWorld());
		if (linkPin != wc.port.pin || !pos.equals(linkPos)) {
			linkPin = wc.port.pin;
			linkPos = pos;
			port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
		}
	}

}
