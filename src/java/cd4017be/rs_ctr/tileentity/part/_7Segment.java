package cd4017be.rs_ctr.tileentity.part;

import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import static cd4017be.lib.render.Util.*;

import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import static cd4017be.rs_ctr.Objects.seg7;
import static cd4017be.rs_ctr.ClientProxy.t_7seg;
import static cd4017be.rs_ctr.ClientProxy.t_blank;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public class _7Segment extends SignalModule implements SignalHandler, IBlockRenderComp {

	public static final String ID = "7seg";

	SignalHandler out;
	Decoding mode = Decoding.DEC_S;
	byte color, dots, digits;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		int m = stack.getMetadata(), h = m >> 1 & 1, w = (m & 1) * (h + 1) + h;
		pos = (byte)((int)Math.floor((4 - w) * x) & 3 | (int)Math.floor((4 - h) * y) << 2 & 12 | w << 4 | h << 6);
	}

	@Override
	public void init(List<MountedPort> ports, int id, IPanel panel) {
		Orientation o = panel.getOrientation();
		double y = getY() + 0.125, x = getX() + 0.125;
		ports.add(new MountedPort(panel, id << 1, SignalHandler.class, false).setLocation(x + getW() - .25, y, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
		if ((pos & 48) != 0)
			ports.add(new MountedPort(panel, id << 1 | 1, SignalHandler.class, true).setLocation(x, y, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.o"));
		digits = (byte)((pos >> 3 & 6) + 2 >> (pos >> 6 & 3));
		super.init(ports, id, panel);
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(seg7);
	}

	@Override
	public void setPortCallback(Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null)
			out.updateSignal(mode.remainder(value, digits));
	}

	@Override
	public void updateSignal(int val) {
		if (val == value) return;
		value = val;
		if (out != null)
			out.updateSignal(mode.remainder(val, digits));
		host.updateDisplay();
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setByte("mode", (byte)mode.ordinal());
		nbt.setByte("cfg", (byte) (dots & 0xf | color << 4));
		return nbt;
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {
		mode = Decoding.values()[(nbt.getByte("mode") & 0xff) % 5];
		int i = nbt.getByte("cfg");
		dots = (byte) (i & 7);
		color = (byte) (i >> 4 & 15);
		super.loadCfg(nbt);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected GuiFrame initGuiFrame(ModularGui gui) {
		GuiFrame frame = new GuiFrame(gui, 128, 43, 1)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 97);
		new TextField(frame, 112, 7, 8, 16, 20, ()-> title, (t)-> gui.sendPkt((byte)0, t)).allowFormat().tooltip("gui.rs_ctr.label");
		new Button(frame, 9, 9, 52, 28, 16, ()-> color, (s)-> gui.sendPkt((byte)1, (byte)s)).texture(247, 72).tooltip("gui.rs_ctr.color");
		new Button(frame, 26, 9, 65, 28, 5, ()-> mode.ordinal(), (s)-> gui.sendPkt((byte)2, (byte)s)).texture(221, 63).tooltip("gui.rs_ctr.encoder#");
		new Button(frame, 26, 9, 96, 28, 5, ()-> dots, (s)-> gui.sendPkt((byte)3, (byte)s)).texture(221, 108).tooltip("gui.rs_ctr.dot");
		return frame;
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		switch(pkt.readByte()) {
		case 0: title = pkt.readString(32); break;
		case 1: color = pkt.readByte(); break;
		case 2:
			mode = Decoding.values()[pkt.readUnsignedByte() % 5];
			if (out != null)
				out.updateSignal(mode.remainder(value, digits));
			break;
		case 3: dots = pkt.readByte(); break;
		default: return;
		}
		host.markDirty(BaseTileEntity.SYNC);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(Orientation o, double x, double y, double z, int light, BufferBuilder buffer) {
		light = brightness(light);
		int vi = buffer.getVertexCount();
		double h = getH() * .5;
		Vec3d p = o.rotate(new Vec3d(getX() + (digits - 1) * h - .5, getY() - .5, .51)).addVector(x + .5, y + .5, z + .5);
		Vec3d dx = o.X.scale(h), dy = o.Y.scale(h / 1.5);
		int color = COLORS[this.color & 15], code = mode.decode(value, digits);
		if (dots > 0) code |= 0x80 << (dots - 1) * 8;
		for (int i = digits - 1; i >= 0; i--)
			if (i == 0 && mode.sign) {
				buffer.addVertexData(texturedRect(p, dx, dy.scale(2), getUV(t_7seg, 12, (code + 1) * 4), getUV(t_7seg, 15, code * 4), color, light));
			} else {
				int u = (code & 3) * 3, v = code & 12; code >>= 4;
				buffer.addVertexData(texturedRect(p.add(dy), dx, dy, getUV(t_7seg, u, v + 2), getUV(t_7seg, u + 3, v), color, light));
				u = (code & 3) * 3; v = (code & 12) + 2; code >>= 4;
				buffer.addVertexData(texturedRect(p, dx, dy, getUV(t_7seg, u, v + 2), getUV(t_7seg, u + 3, v), color, light));
				p = p.subtract(dx);
			}
		IntArrayModel m = new IntArrayModel(extractData(buffer, vi, buffer.getVertexCount()), color, light);
		m.origin((float)x, (float)y, (float)z);
		renderCache = m;
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected int brightness(int light) {
		return light & 0xff0000 | 0xf0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		IPanel host = this.host;
		if (host == null) return;
		Orientation o = host.getOrientation();
		quads.add(new BakedQuad(texturedRect(o.rotate(new Vec3d(getX() - .5, getY() - .5, .505)).addVector(.5, .5, .5), o.X.scale(getW()), o.Y.scale(getH() / 1.5), getUV(t_blank, 0, 0), getUV(t_blank, 16, 16), 0xff3f3f3f, 0), -1, o.back, t_blank, true, DefaultVertexFormats.BLOCK));
	}

	static final byte[] DIGITS = {
			0b1111110, 0b0011000, 0b1101101, 0b0111101, //0, 1, 2, 3
			0b0011011, 0b0110111, 0b1110111, 0b0011100, //4, 5, 6, 7
			0b1111111, 0b0111111, 0b1011111, 0b1110011, //8, 9, A, B
			0b1100110, 0b1111001, 0b1100111, 0b1000111, //C, D, E, F
		};

	static final int[] COLORS = {
			//red      , orange     , yellow     , green      , cyan       , blue       , magenta    , white
			0xff_0000ff, 0xff_007fff, 0xff_00ffff, 0xff_00ff00, 0xff_bfbf00, 0xff_ff0000, 0xff_bf00bf, 0xff_bfbfbf, //normal
			0xff_7f7fff, 0xff_7fbfff, 0xff_7fffff, 0xff_7fff7f, 0xff_ffff7f, 0xff_ff7f7f, 0xff_ff7fff, 0xff_ffffff, //bleached
			0xff_000040, 0xff_002040, 0xff_004040, 0xff_004000, 0xff_303000, 0xff_400000, 0xff_300030, 0xff_303030, //dark
			0xff_202040, 0xff_203040, 0xff_204040, 0xff_204020, 0xff_404020, 0xff_402020, 0xff_402040, 0xff_404040,
		};

	static final int[] EXP10 = {1, 10, 100, 1000, 10000};

	enum Decoding {
		RAW(false),
		RAW_S(true),
		DEC(false) {
			@Override
			int decode(int val, int digits) {
				if (val < 0) {
					if (val == Integer.MIN_VALUE) return 0;
					val = -val;
				} else if (val == Integer.MAX_VALUE) return 0;
				int code = DIGITS[val % 10];
				if (val < 10) return code;
				code |= DIGITS[(val /= 10) % 10] << 8;
				if (val < 10) return code;
				code |= DIGITS[(val /= 10) % 10] << 16;
				if (val < 10) return code;
				return code | DIGITS[(val / 10) % 10] << 24;
			}
			@Override
			int remainder(int val, int digits) {
				digits = EXP10[digits];
				return val < 0 ?
						(val <= -digits && val != Integer.MIN_VALUE ? val / digits : Integer.MIN_VALUE)
						: (val >= digits && val != Integer.MAX_VALUE ? val / digits : Integer.MAX_VALUE);
			}
		}, DEC_S(true) {
			@Override
			int decode(int val, int digits) {
				int code = 0, s = digits - 1 << 3;
				if (val < 0) {
					code = 2 << s;
					if (val == Integer.MIN_VALUE) return code;
					val = -val;
				} else if (val == Integer.MAX_VALUE) return code;
				for (int i = 0; i < s; i+=8, val /= 10) {
					code |= DIGITS[val % 10] << i;
					if (val < 10) return code;
				}
				code |= 1 << s;
				if (val >= 2) code &= 3 << s;
				return code;
			}
		}, HEX(false) {
			@Override
			int decode(int val, int digits) {
				int code = 0;
				for (int i = 0; i < digits; i++, val >>>= 4)
					code |= DIGITS[val & 15] << (i << 3);
				return code;
			}
			@Override
			int remainder(int val, int digits) {
				return val >>> (digits << 2);
			}
		};

		final boolean sign;

		private Decoding(boolean sign) {
			this.sign = sign;
		}

		int decode(int val, int digits) {
			return val;
		}

		int remainder(int val, int digits) {
			return val;
		}

	}

	@Override
	public Object getState(int id) {
		return id == 0 ? value : mode.remainder(value, digits);
	}

}
