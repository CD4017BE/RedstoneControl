package cd4017be.rs_ctr.tileentity.part;

import java.util.List;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.comp.*;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.network.*;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/** 
 * @author CD4017BE */
public abstract class Socket extends Module implements IStateInteractionHandler {

	/** x[0..3-w], y[0..3-h] */
	protected byte pos;
	protected String title = "";

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setByte("pos", pos);
		nbt.setString("title", title);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		pos = nbt.getByte("pos");
		loadCfg(nbt);
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {
		title = nbt.getString("title");
	}

	protected abstract String portLabel(boolean out);
	protected abstract Class<?> type();

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		double x = getX() + .125, y = getY() + .125;
		Orientation o = panel.getOrientation();
		boolean out = (pos & 16) != 0;
		ports.add(new MountedPort(panel, idx<<1, type(), !out).setLocation(x, y, .75, EnumFacing.NORTH, o).setName(portLabel(!out)));
		ports.add(new MountedPort(panel, idx<<1 | 1, type(), out).setLocation(x, y, 1.002, EnumFacing.SOUTH, o).setName('\\' + title));
		super.init(ports, idx, panel);
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		int gx = (int)Math.floor(x * 4F), gy = (int)Math.floor(y * 4F);
		pos = (byte) (gx & 3 | gy << 2 & 12 | (int)((x + y) * 4F - (float)(gx + gy)) << 4 & 16);
		title = TooltipUtil.translate(portLabel((pos & 16) != 0));
	}

	@Override
	public int getBounds() {
		return 1 << (pos & 15);
	}

	protected double getX() {
		return (double)(pos & 3) * .25;
	}

	protected double getY() {
		return (double)(pos >> 2 & 3) * .25;
	}

	@Override
	public void writeSync(PacketBuffer buf, boolean init) {}

	@Override
	public void readSync(PacketBuffer buf) {}

	@Override
	public AdvancedContainer getCfgContainer(EntityPlayer player) {
		return new AdvancedContainer(this, StateSynchronizer.builder().build(host.world().isRemote), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getCfgScreen(EntityPlayer player) {
		ModularGui gui = new ModularGui(getCfgContainer(player));
		GuiFrame frame = new GuiFrame(gui, 80, 53, 3)
		.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
		.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 0, 151);
		new TextField(frame, 64, 7, 8, 16, 16, ()-> title, (t)-> gui.sendPkt(t)).allowFormat().tooltip("gui.rs_ctr.label");
		gui.compGroup = frame;
		return gui;
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		title = pkt.readString(32);
		host.markDirty(BaseTileEntity.SYNC);
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		IPanel host = this.host;
		return !player.isDead && host != null && player.getDistanceSqToCenter(host.pos()) < 256;
	}

}
