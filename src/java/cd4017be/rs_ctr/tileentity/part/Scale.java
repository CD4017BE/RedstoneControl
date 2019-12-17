package cd4017be.rs_ctr.tileentity.part;

import static cd4017be.lib.render.Util.getUV;
import static cd4017be.lib.render.Util.texturedRect;
import static cd4017be.rs_ctr.ClientProxy.t_blank;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.com.SignalHandler;
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
public class Scale extends SignalModule
implements SignalHandler, IBlockRenderComp {

	public static final String ID = "scale";

	double[] gains = {1, 2, 5, 10, 20, 50, 100, 200, 500};

	SignalHandler out = SignalHandler.NOP;
	String unit = "";
	long scale = 0x1_0000_0000L;
	double base = 1.0;
	int gain = 54, in;
	{
		title = "Scale";
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
	public void updateSignal(int value) {
		in = value;
		value = (int)(value * scale >> 32);
		if(value == this.value) return;
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
	public boolean onInteract(
		EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim
	) {
		if(super.onInteract(player, hit, side, aim)) return true;
		int d = increment(aim, host.getOrientation());
		if (d == 0) return false;
		if (player.isSneaking()) gain = 54;
		d += gain & 0x7f;
		if (d < 0) d = 0;
		else if (d > 107) d = 107;
		gain = gain & 0x80 | d;
		setScale();
		host.updateDisplay();
		return true;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		IPanel host = this.host;
		if(host == null) return null;
		int val = increment(aim, host.getOrientation());
		return val == 0 ? null : Pair.of(aim, val < 0 ? val < -1 ? "--" : "-" : val == 1 ? "+" : "++");
	}

	private int increment(Vec3d aim, Orientation o) {
		double d = aim.subtract(.5, .5, .5).dotProduct(o.Y) + .5 - getY();
		if (d < .0625 || d > .1875) return 0;
		d = aim.subtract(.5, .5, .5).dotProduct(o.X) + .5 - getX();
		return d < .25 ? d < .125 ? -9 : -1
			: d < .375 ? 1 : 9;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setDouble("base", base);
		nbt.setByte("gain", (byte)gain);
		nbt.setString("unit", unit);
		return nbt;
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {
		base = nbt.getDouble("base");
		gain = nbt.getByte("gain");
		unit = nbt.getString("unit");
		setScale();
		super.loadCfg(nbt);
	}

	private void setScale() {
		scale = (gain & 0x80) == 0
			? (long)(base * 0x1p32 * gain())
			: (long)(base * 0x1p32 / gain());
		updateSignal(in);
	}

	private double gain() {
		int gain = this.gain & 0x7f;
		return gains[gain % 9] * TooltipUtil.ScaleUnits[gain / 9];
	}

	@Override
	public void writeSync(PacketBuffer buf, boolean init) {
		buf.writeByte(gain);
	}

	@Override
	public void readSync(PacketBuffer buf) {
		gain = buf.readByte();
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
		String s = ((gain & 0x80) != 0 ? '/' : '*') + TooltipUtil.formatNumber(gain(), 3, 0);
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
		GuiFrame frame = new GuiFrame(gui, 80, 52, 4)
		.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
		.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 0, 204);
		new TextField(
			frame, 64, 7, 8, 16, 20, () -> title, (t) -> gui.sendPkt((byte)0, t)
		).allowFormat().tooltip("gui.rs_ctr.label");
		new TextField(
			frame, 55, 7, 17, 37, 16, () -> unit, (t) -> gui.sendPkt((byte)3, t)
		).allowFormat().tooltip("gui.rs_ctr.unit");
		new TextField(frame, 64, 7, 8, 28, 12, () -> Double.toString(base), (t) -> {
			try {
				gui.sendPkt((byte)1, Double.parseDouble(t));
			} catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.scale");
		new Button(
			frame, 9, 9, 7, 36, 2, ()-> gain >> 7 & 1,
			(s)-> gui.sendPkt((byte)2, (byte)(gain ^ 0x80))
		).texture(218, 36).tooltip("gui.rs_ctr.div#");
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
			base = pkt.readDouble();
			setScale();
			break;
		case 2:
			gain = pkt.readByte();
			setScale();
			break;
		case 3:
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
		return new ItemStack(Objects.scale);
	}

}
