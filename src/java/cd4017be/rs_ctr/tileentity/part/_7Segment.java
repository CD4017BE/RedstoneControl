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
import cd4017be.rs_ctr.Objects;

import static cd4017be.rs_ctr.render.PanelRenderer.*;
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
	Decoding mode = Decoding.DEC;
	byte color, dots;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		pos = (byte) ((int)Math.floor(y * 3.) << 2 & 12 | 0x70);
	}

	@Override
	public void init(List<MountedPort> ports, int id, IPanel panel) {
		Orientation o = panel.getOrientation();
		double y = getY() + 0.125;
		ports.add(new MountedPort(panel, id << 1, SignalHandler.class, false).setLocation(0.875, y, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
		ports.add(new MountedPort(panel, id << 1 | 1, SignalHandler.class, true).setLocation(0.125, y, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.o"));
		super.init(ports, id, panel);
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.seg7);
	}

	@Override
	public void setPortCallback(Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null)
			out.updateSignal(mode.remainder(value));
	}

	@Override
	public void updateSignal(int val) {
		if (val == value) return;
		value = val;
		if (out != null)
			out.updateSignal(mode.remainder(val));
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
				out.updateSignal(mode.remainder(value));
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
		Vec3d p = o.rotate(new Vec3d(.25, getY() - .5, .51)).addVector(x + .5, y + .5, z + .5);
		Vec3d dx = o.X.scale(0.25), dy = o.Y.scale(1./6.);
		int color = COLORS[this.color & 15], code = mode.decode(value);
		if (dots > 0) code |= 0x80 << (dots - 1) * 8;
		for (int i = 0; i < 4; i++)
			if (i == 3 && mode.sign) {
				buffer.addVertexData(texturedRect(p, dx, dy.scale(2), getUV(seg7, 12, (code + 1) * 4), getUV(seg7, 15, code * 4), color, light));
			} else {
				int u = (code & 3) * 3, v = code & 12; code >>= 4;
				buffer.addVertexData(texturedRect(p.add(dy), dx, dy, getUV(seg7, u, v + 2), getUV(seg7, u + 3, v), color, light));
				u = (code & 3) * 3; v = (code & 12) + 2; code >>= 4;
				buffer.addVertexData(texturedRect(p, dx, dy, getUV(seg7, u, v + 2), getUV(seg7, u + 3, v), color, light));
				p = p.subtract(dx);
			}
		renderCache = new IntArrayModel(extractData(buffer, vi, buffer.getVertexCount()), color, light);
		renderCache.origin((float)x, (float)y, (float)z);
		return true;
	}

	@Override
	protected int brightness(int light) {
		return light & 0xff0000 | 0xf0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		Orientation o = host.getOrientation();
		quads.add(new BakedQuad(texturedRect(o.rotate(new Vec3d(-.5, getY() - .5, .505)).addVector(.5, .5, .5), o.X, o.Y.scale(1./3.), getUV(blank, 0, 0), getUV(blank, 16, 16), 0xff3f3f3f, 0), -1, o.back, blank, true, DefaultVertexFormats.BLOCK));
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

	enum Decoding {
		RAW(false),
		RAW_S(true),
		DEC(false) {
			@Override
			int decode(int val) {
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
			int remainder(int val) {
				return val < 0 ?
						(val <= -10000 && val != Integer.MIN_VALUE ? val / 10000 : Integer.MIN_VALUE)
						: (val >= 10000 && val != Integer.MAX_VALUE ? val / 10000 : Integer.MAX_VALUE);
			}
		}, DEC_S(true) {
			@Override
			int decode(int val) {
				int code = 0;
				if (val < 0) {
					code = 0x2000000;
					if (val == Integer.MIN_VALUE) return code;
					val = -val;
				} else if (val == Integer.MAX_VALUE) return code;
				code |= DIGITS[val % 10];
				if (val < 10) return code;
				code |= DIGITS[(val /= 10) % 10] << 8;
				if (val < 10) return code;
				code |= DIGITS[(val /= 10) % 10] << 16;
				if (val < 10) return code;
				code |= 0x1000000;
				if (val >= 20) code &= 0x3000000;
				return code;
			}
		}, HEX(false) {
			@Override
			int decode(int val) {
				int code = 0;
				for (int i = 0; i < 32; i+=8, val >>>= 4)
					code |= DIGITS[val & 15] << i;
				return code;
			}
			@Override
			int remainder(int val) {
				return val >>> 16;
			}
		};

		final boolean sign;

		private Decoding(boolean sign) {
			this.sign = sign;
		}

		int decode(int val) {
			return val;
		}

		int remainder(int val) {
			return val;
		}

	}

}
