package cd4017be.rs_ctr.tileentity.part;

import static cd4017be.lib.render.Util.getUV;
import static cd4017be.lib.render.Util.texturedRect;
import static cd4017be.rs_ctr.ClientProxy.t_blank;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author cd4017be */
public class Offset extends SignalModule
implements SignalHandler, IBlockRenderComp {

	public static final String ID = "offset";

	SignalHandler out = SignalHandler.NOP;
	String unit = "";
	int offset, increment = 1, exp;
	{
		title = "Offset";
	}

	@Override
	public void updateSignal(int value) {
		value += offset;
		if (value == this.value) return;
		out.updateSignal(this.value = value);
	}

	@Override
	public void setPortCallback(Object callback) {
		if(callback instanceof SignalHandler) {
			SignalHandler h = (SignalHandler)callback;
			h.updateSignal(value);
			out = h;
		} else out = SignalHandler.NOP;
	}

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		ports.add(
			new MountedPort(panel, idx << 1, SignalHandler.class, false)
			.setLocation(
				getX() + .375, getY() + .125, .75, EnumFacing.NORTH,
				panel.getOrientation()
			)
			.setName("port.rs_ctr.i")
		);
		ports.add(
			new MountedPort(panel, idx << 1 | 1, SignalHandler.class, true)
			.setLocation(
				getX() + .125, getY() + .125, .75, EnumFacing.NORTH,
				panel.getOrientation()
			)
			.setName("port.rs_ctr.o")
		);
		super.init(ports, idx, panel);
	}

	@Override
	public boolean onInteract(
		EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim
	) {
		if(super.onInteract(player, hit, side, aim)) return true;
		Orientation o = host.getOrientation();
		int s = selection(aim, o);
		if (s == 0) return false;
		int d = increment(aim, o);
		int in = value - offset;
		offset += d;
		updateSignal(in);
		host.updateDisplay();
		return true;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		IPanel host = this.host;
		if(host == null) return null;
		Orientation o = host.getOrientation();
		if (selection(aim, o) == 0) return null;
		double val = increment(aim, o) * scale();
		return Pair.of(aim, TooltipUtil.formatNumber(val, 3, 1.0, true, true));
	}

	private int selection(Vec3d aim, Orientation o) {
		double d = aim.subtract(.5, .5, .5).dotProduct(o.Y) + .5 - getY();
		if (d < .0625 || d > .1875) return 0;
		return 1;
	}

	private int increment(Vec3d aim, Orientation o) {
		double dx = aim.subtract(.5, .5, .5).dotProduct(o.X) + .5 - getX();
		return dx < .25 ? dx < .125 ? -10 * increment : -increment
			: dx < .375 ? increment : 10 * increment;
	}

	private double scale() {
		if (exp < -12 || exp > 11) return Double.NaN;
		return TooltipUtil.ScaleUnits[(exp + 18) / 3] * TooltipUtil.exp10[Math.floorMod(exp, 3)];
	}

	@Override
	public void writeSync(PacketBuffer buf) {
		buf.writeInt(offset);
	}

	@Override
	public void readSync(PacketBuffer buf) {
		offset = buf.readInt();
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("ofs", offset);
		nbt.setInteger("incr", increment);
		nbt.setByte("exp", (byte)exp);
		nbt.setString("unit", unit);
		return nbt;
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {
		super.loadCfg(nbt);
		offset = nbt.getInteger("ofs");
		increment = nbt.getInteger("incr");
		exp = nbt.getByte("exp");
		unit = nbt.getString("unit");
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(
		Orientation o, double x, double y, double z, int light,
		BufferBuilder buffer
	) {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		IPanel host = this.host;
		if(host == null) return;
		Orientation o = host.getOrientation();
		quads.add(
			new BakedQuad(
				texturedRect(
					o.rotate(new Vec3d(getX() - .5 + .0625, getY() - .5, .505)).addVector(.5, .5, .5),
					o.X.scale(.375), o.Y.scale(1./6./*.1875*/),
					getUV(t_blank, 0, 0), getUV(t_blank, 16, 16), 0xff3f3f3f, 0
				), -1, o.back, t_blank, true, DefaultVertexFormats.BLOCK
			)
		);
	}

	@Override
	public void drawText(FontRenderer fr) {
		int x = (pos & 3) * 32, y = (3 - (pos >> 2 & 3)) * 32, w = 64;
		y++;
		fr.drawString(
			title, x + (w - fr.getStringWidth(title)) / 2, y + 1, 0xff000000
		);
		GlStateManager.translate(0, 0, -.005);
		String s = TooltipUtil.formatNumber((double)offset * scale(), 6, 1.0, true, true);
		fr.drawString(
			s, x + (w - fr.getStringWidth(s)) / 2, y + 12, 0xffc0c000
		);
		fr.drawString(
			unit, x + (w - fr.getStringWidth(unit)) / 2, y + 23, 0xffc0c000
		);
		GlStateManager.translate(0, 0, .005);
	}

	@Override
	protected GuiFrame initGuiFrame(ModularGui gui) {
		GuiFrame frame = new GuiFrame(gui, 128, 66, 7)
		.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
		.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 31);
		new TextField(
			frame, 112, 7, 8, 16, 20, () -> title, (t) -> gui.sendPkt((byte)0, t)
		).allowFormat().tooltip("gui.rs_ctr.label");
		new TextField(
			frame, 74, 7, 45, 29, 16, () -> unit, (t) -> gui.sendPkt((byte)4, t)
		).tooltip("gui.rs_ctr.unit");
		new TextField(frame, 64, 7, 56, 42, 12, () -> Integer.toString(increment), (t) -> {
			try {
				gui.sendPkt((byte)1, Integer.parseInt(t));
			} catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.incr");
		new TextField(frame, 64, 7, 56, 51, 12, () -> Integer.toString(offset), (t) -> {
			try {
				gui.sendPkt((byte)2, Integer.parseInt(t));
			} catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.ofs");
		new Button(
			frame, 20, 9, 7, 28, 3, ()-> Math.floorMod(exp, 3),
			(s)-> gui.sendPkt((byte)3, (byte)(s + Math.floorDiv(exp, 3) * 3))
		).texture(227, 36).tooltip("gui.rs_ctr.uscale");
		new Button(
			frame, 9, 9, 31, 28, 8, ()-> (exp + 12) / 3,
			(s)-> gui.sendPkt((byte)3, (byte)(s * 3 - 12 + Math.floorMod(exp, 3)))
		).texture(247, 0).tooltip("gui.rs_ctr.uscale");
		return frame;
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender)
	throws Exception {
		switch(pkt.readByte()) {
		case 0:
			title = pkt.readString(32);
			break;
		case 1:
			increment = pkt.readInt();
			break;
		case 2:
			int in = value - offset;
			offset = pkt.readInt();
			updateSignal(in);
			break;
		case 3:
			exp = pkt.readByte();
			break;
		case 4:
			unit = pkt.readString(32);
			break;
		default: return;
		}
		host.markDirty(BaseTileEntity.SYNC);
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		pos = (byte)(16 | (int)Math.floor(x * 3.) & 3
		| (int)Math.floor(y * 4.) << 2 & 12);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.offset);
	}

}
