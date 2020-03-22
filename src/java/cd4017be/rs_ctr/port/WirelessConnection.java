package cd4017be.rs_ctr.port;

import java.util.List;

import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.IConnector;
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
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class WirelessConnection extends Plug implements ITagableConnector, IBlockRenderComp {

	private final WireType type;
	private DimPos linkPos;
	private int linkPin;
	private boolean dropsItem;
	private String tag;

	public WirelessConnection(WireType type) {
		this.type = type;
	}

	public WirelessConnection(DimPos linkPos, int linkPin, boolean drop, WireType type) {
		this(type);
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
		if (tag != null) nbt.setString("tag", tag);
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
			return ITagableConnector.super.displayInfo(port, linkID)
				+ "\n[" + linkPos.getX() + ", " + linkPos.getY() + ", " + linkPos.getZ() + "]\n"
				+ DimensionManager.getProviderType(linkPos.dimId).getName();
		} catch (IllegalArgumentException e) {
			return "\n" + e.getMessage();
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), type.wirelessModel());
	}

	@Override
	public void onRemoved(MountedPort port, EntityPlayer player) {
		World world = port.getWorld();
		BlockPos pos = port.getPos();
		Port p = IPortProvider.getPort(linkPos.getWorldServer(), linkPos, linkPin);
		if (p instanceof MountedPort) {
			IConnector c = ((MountedPort)p).getConnector();
			if (c instanceof WirelessConnection) {
				WirelessConnection wc = (WirelessConnection)c;
				if (wc.linkPos.equals(pos) && wc.linkPos.dimId == world.provider.getDimension() && wc.linkPin == port.pin) {
					if (wc.dropsItem && !this.dropsItem) {
						this.dropsItem = true; wc.dropsItem = false;
					}
					((MountedPort)p).setConnector(null, player);
				}
			}
		}
		if (dropsItem) super.onRemoved(port, player);
		port.disconnect();
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(type.wirelessItem);
	}

	@Override
	public void setTag(MountedPort port, String tag) {
		if (this.tag != null ? this.tag.equals(tag) : tag == null) return;
		this.tag = tag;
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
		Port p = IPortProvider.getPort(linkPos.getWorldServer(), linkPos, linkPin);
		if (p instanceof MountedPort) {
			IConnector c = ((MountedPort)p).getConnector();
			if (c instanceof WirelessConnection) {
				((WirelessConnection)c).tag = tag;
				p.owner.onPortModified(p, IPortProvider.E_CON_UPDATE);
			}
		}
	}

	@Override
	public String getTag() {
		return tag;
	}

}
