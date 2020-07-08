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

	int max = 15, min = 0, exp = 0;
	String unit = "";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		int m = stack.getMetadata();
		pos = (byte)(0x50
			| ((m & 1) == 0 ? (int)Math.floor(x * 3F) & 3 : 0x20)
			| ((m & 2) == 0 ? (int)Math.floor(y * 3F) << 2 & 12 : 0x80)
		);
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.pointer_dsp);
	}

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		Orientation o = panel.getOrientation();
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, false).setLocation(getX() + getW() * .5, getY() + getH() * .5, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
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
		nbt.setString("unit", unit);
		return nbt;
	}

	@Override
	public void loadCfg(NBTTagCompound nbt) {
		max = nbt.getInteger("max");
		min = nbt.getInteger("min");
		exp = nbt.getByte("exp");
		unit = nbt.getString("unit");
		super.loadCfg(nbt);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(Orientation o, double x, double y, double z, int light, BufferBuilder buffer) {
		renderCache = Layout.of(pos >> 5 & 1 | pos >> 6 & 2)
		.getPointer(((double)value - (double)min) / ((double)max - (double)min), light, (float)getX(), (float)getY()).rotated(o);
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		IPanel host = this.host;
		if (host == null) return;
		int color = 0xff000000;
		if (unit.length() >= 2 && unit.charAt(0) == '\u00a7') {
			int i = Minecraft.getMinecraft().fontRenderer.getColorCode(Character.toLowerCase(unit.charAt(1)));
			if (i != -1)
				color |= i & 0xff00 | i >> 16 & 0xff | i << 16 & 0xff0000;
		}
		Layout.of(pos >> 5 & 1 | pos >> 6 & 2)
		.drawScale(quads, host.getOrientation(), min, max, exp, color, (float)getX(), (float)getY());
	}

	static final char[] PREFIX = {'p', 'n', '\u03bc', 'm', ' ', 'k', 'M', 'G', 'T', 'P', 'E'};

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		int x = (pos & 3) * 32, y = 64 - (pos & 12) * 8;
		int exp = this.exp;
		for (int mag = Math.max(Math.abs(min), Math.abs(max)); mag >= 10; mag /= 10) exp++;
		exp = (exp + 12) / 3;
		String unit = this.unit;
		if (exp != 4 && exp >= 0 && exp < PREFIX.length)
			unit = FontRenderer.getFormatFromString(unit) + PREFIX[exp] + unit;
		switch(pos >> 5 & 5) {
		case 0:
			fr.drawSplitString(title, x + 1, y + 1, 50, 0xff000000);
			fr.drawString(unit, x + (84 - fr.getStringWidth(unit)) / 2, y + 44, 0xff000000);
			break;
		case 1:
			fr.drawString(title, (128 - fr.getStringWidth(title)) / 2, y, 0xff000000);
			fr.drawString(unit, (128 - fr.getStringWidth(unit)) / 2, y + 42, 0xff000000);
			break;
		case 4:
			fr.drawSplitString(title, x + 8, 8, 50, 0xff000000);
			fr.drawString(unit, x + (84 - fr.getStringWidth(unit)) / 2, 62, 0xff000000);
			break;
		default:
			fr.drawString(title, (128 - fr.getStringWidth(title)) / 2, 10, 0xff000000);
			fr.drawString(unit, (128 - fr.getStringWidth(unit)) / 2, 88, 0xff000000);
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
		new Button(frame, 20, 9, 7, 28, 3, ()-> Math.floorMod(exp, 3), (s)-> gui.sendPkt(A_EXP, (byte)(s + Math.floorDiv(exp, 3) * 3))).texture(227, 36).tooltip("gui.rs_ctr.uscale");
		new Button(frame, 9, 9, 31, 28, 8, ()-> (exp + 12) / 3, (s)-> gui.sendPkt(A_EXP, (byte)(s * 3 - 12 + Math.floorMod(exp, 3)))).texture(247, 0).tooltip("gui.rs_ctr.uscale");
		return frame;
	}

	static final byte A_MIN = 0, A_MAX = 1, A_EXP = 2, A_UNIT = 4, A_TITLE = 5;

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		switch(pkt.readByte()) {
		case A_MIN: min = pkt.readInt(); break;
		case A_MAX: max = pkt.readInt(); break;
		case A_EXP: exp = pkt.readByte(); break;
		case A_UNIT: unit = pkt.readString(32); break;
		case A_TITLE: title = pkt.readString(32); break;
		default: return;
		}
		host.markDirty(BaseTileEntity.REDRAW);
	}

}
