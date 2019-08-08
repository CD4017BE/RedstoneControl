package cd4017be.rs_ctr.tileentity.part;

import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.render.PanelRenderer.Layout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public class PointerDisplay extends SignalModule implements SignalHandler, IBlockRenderComp {

	public static final String ID = "pointer";

	byte type;
	int max = 15, min = 0, exp = 0;
	String unit = "";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		pos = (byte) 0xf0;
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.pointer_dsp);
	}

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		Orientation o = panel.getOrientation();
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, false).setLocation(0.5, 0.5, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
		super.init(ports, idx, panel);
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
		nbt.setInteger("max", max);
		nbt.setInteger("min", min);
		nbt.setByte("exp", (byte)exp);
		nbt.setByte("type", type);
		nbt.setString("unit", unit);
		return nbt;
	}

	@Override
	public void loadCfg(NBTTagCompound nbt) {
		max = nbt.getInteger("max");
		min = nbt.getInteger("min");
		exp = nbt.getByte("exp");
		type = nbt.getByte("type");
		unit = nbt.getString("unit");
		super.loadCfg(nbt);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(Orientation o, double x, double y, double z, int light, BufferBuilder buffer) {
		renderCache = Layout.of(type).getPointer(((double)value - (double)min) / ((double)max - (double)min), light).rotated(o);
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		int color = 0xff000000;
		if (unit.length() >= 2 && unit.charAt(0) == '\u00a7') {
			int i = Minecraft.getMinecraft().fontRenderer.getColorCode(Character.toLowerCase(unit.charAt(1)));
			if (i != -1)
				color |= i & 0xff00 | i >> 16 & 0xff | i << 16 & 0xff0000;
		}
		Layout.of(type).drawScale(quads, host.getOrientation(), min, max, exp, color);
	}

	static final char[] PREFIX = {'p', 'n', '\u03bc', 'm', ' ', 'k', 'M', 'G', 'T', 'P', 'E'};

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		int exp = this.exp;
		for (int mag = Math.max(Math.abs(min), Math.abs(max)); mag >= 10; mag /= 10) exp++;
		exp = (exp + 12) / 3;
		String unit = this.unit;
		if (exp != 4 && exp >= 0 && exp < PREFIX.length)
			unit = FontRenderer.getFormatFromString(unit) + PREFIX[exp] + unit;
		int w = 128;
		if (type == 1) {
			fr.drawString(title, (w - fr.getStringWidth(title)) / 2, 10, 0xff000000);
			fr.drawString(unit, (w - fr.getStringWidth(unit)) / 2, 88, 0xff000000);
		} else {
			fr.drawSplitString(title, 9, 10, 80, 0xff000000);
			fr.drawString(unit, (w - fr.getStringWidth(unit) + 40) / 2, 88, 0xff000000);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected GuiFrame initGuiFrame(ModularGui gui) {
		GuiFrame frame = new GuiFrame(gui, 128, 66, 7)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 31);
		new TextField(frame, 112, 7, 8, 16, 20, ()-> title, (t)-> gui.sendPkt(A_TITLE, t)).allowFormat().tooltip("gui.rs_ctr.label");
		new TextField(frame, 75, 7, 45, 29, 12, ()-> unit, (t)-> gui.sendPkt(A_UNIT, t)).allowFormat().tooltip("gui.rs_ctr.unit");
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
		return frame;
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
