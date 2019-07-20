package cd4017be.rs_ctr.tileentity.part;

import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.render.PanelRenderer.Layout;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public class PointerDisplay extends Module implements SignalHandler, ITESRenderComp, IBlockRenderComp, IInteractiveComponent, IStateInteractionHandler {

	public static final String ID = "pointer";

	byte type;
	int max = 15, min = 0, exp = 0;
	String title = "", unit = "";
	int value;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public int getBounds() {
		return 0xffff;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
	}

	@Override
	public ItemStack onRemove() {
		// TODO Auto-generated method stub
		return ItemStack.EMPTY;
	}

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		Orientation o = panel.getOrientation();
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, false).setLocation(0.5, 0.5, 0, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
		super.init(ports, idx, panel);
	}

	@Override
	public SignalHandler getPortCallback() {
		return this;
	}

	@Override
	public void setPortCallback(Object callback) {
	}

	@Override
	public void resetInput() {
		updateSignal(0);
	}

	@Override
	public void updateSignal(int value) {
		if (this.value == value) return;
		this.value = value;
		host.updateDisplay();
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("val", value);
		nbt.setInteger("max", max);
		nbt.setInteger("min", min);
		nbt.setByte("exp", (byte)exp);
		nbt.setByte("type", type);
		nbt.setString("title", title);
		nbt.setString("unit", unit);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		value = nbt.getInteger("val");
		max = nbt.getInteger("max");
		min = nbt.getInteger("min");
		exp = nbt.getByte("exp");
		type = nbt.getByte("type");
		title = nbt.getString("title");
		unit = nbt.getString("unit");
		if (host != null && host.world().isRemote)
			model = null;
	}

	@Override
	public void writeSync(PacketBuffer buf) {
		buf.writeInt(value);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void readSync(PacketBuffer buf) {
		int val = buf.readInt();
		if (val != value) {
			value = val;
			model = null;
		}
	}

	@SideOnly(Side.CLIENT)
	IntArrayModel model;

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (model == null)
			model = Layout.of(type).getPointer(((double)value - (double)min) / ((double)max - (double)min), light).rotated(host.getOrientation());
		model.setBrightness(light);
		buffer.addVertexData(model.translated((float)x, (float)y, (float)z).vertexData);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		Layout.of(type).drawScale(quads, host.getOrientation(), min, max, exp);//0xff000000
	}

	static final char[] PREFIX = {'p', 'n', '\u03bc', 'm', ' ', 'k', 'M', 'G', 'T', 'P', 'E'};

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		int exp = this.exp;
		for (int mag = Math.max(Math.abs(min), Math.abs(max)); mag >= 10; mag /= 10) exp++;
		exp = (exp + 12) / 3;
		String unit = exp != 4 && exp >= 0 && exp < PREFIX.length ? PREFIX[exp] + this.unit : this.unit;
		int w = 128;
		if (type == 1) {
			fr.drawString(title, (w - fr.getStringWidth(title)) / 2, 12, 0xff000000);
			fr.drawString(unit, (w - fr.getStringWidth(unit)) / 2, 96, 0xff000000);
		} else {
			fr.drawSplitString(title, 9, 12, 80, 0xff000000);
			fr.drawString(unit, (w - fr.getStringWidth(unit) + 40) / 2, 96, 0xff000000);
		}
	}

	@Override
	public AdvancedContainer getCfgContainer(EntityPlayer player) {
		return new AdvancedContainer(this, StateSynchronizer.builder().build(host.world().isRemote), player);
	}

	@Override
	public ModularGui getCfgScreen(EntityPlayer player) {
		ModularGui gui = new ModularGui(getCfgContainer(player));
		GuiFrame frame = new GuiFrame(gui, 128, 66, 7)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 31);
		new TextField(frame, 112, 7, 8, 16, 20, ()-> title, (t)-> gui.sendPkt(A_TITLE, t)).tooltip("gui.rs_ctr.label");
		new TextField(frame, 75, 7, 45, 29, 12, ()-> unit, (t)-> gui.sendPkt(A_UNIT, t)).tooltip("gui.rs_ctr.unit");
		new TextField(frame, 64, 7, 56, 42, 12, ()-> Integer.toString(max), (t)-> {
			try {gui.sendPkt(A_MAX, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.max");
		new TextField(frame, 64, 7, 56, 51, 12, ()-> Integer.toString(min), (t)-> {
			try {gui.sendPkt(A_MIN, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.min");
		new Button(frame, 18, 18, 7, 41, 2, ()-> type, (s)-> gui.sendPkt(A_TYPE, (byte)s)).texture(229, 0).tooltip("gui.rs_ctr.style");
		new Button(frame, 20, 9, 7, 28, 3, ()-> Math.floorMod(exp, 3), (s)-> gui.sendPkt(A_EXP, (byte)(s + Math.floorDiv(exp, 3) * 3))).texture(227, 36).tooltip("gui.rs_ctr.uscale");
		new Button(frame, 9, 9, 31, 28, 8, ()-> (exp + 12) / 3, (s)-> gui.sendPkt(A_EXP, (byte)(s * 3 - 12 + Math.floorMod(exp, 3)))).texture(247, 0).tooltip("gui.rs_ctr.uscale");
		gui.compGroup = frame;
		return gui;
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return !player.isDead && player.getDistanceSqToCenter(host.pos()) < 256;
	}

	static final byte A_MIN = 0, A_MAX = 1, A_EXP = 2, A_TYPE = 3, A_UNIT = 4, A_TITLE = 5;

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		switch(pkt.readByte()) {
		case A_MIN: min = pkt.readInt(); break;
		case A_MAX: max = pkt.readInt(); break;
		case A_EXP: exp = pkt.readByte(); break;
		case A_TYPE: type = pkt.readByte(); break;
		case A_UNIT: unit = pkt.readString(32); break;
		case A_TITLE: title = pkt.readString(32); break;
		default: return;
		}
		host.markDirty(BaseTileEntity.REDRAW);
	}

}
