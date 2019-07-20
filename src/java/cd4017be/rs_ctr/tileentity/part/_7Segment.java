package cd4017be.rs_ctr.tileentity.part;

import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import static cd4017be.lib.render.Util.*;

import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
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
import cd4017be.rs_ctr.Objects;

import static cd4017be.rs_ctr.render.PanelRenderer.*;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public class _7Segment extends Module implements SignalHandler, ITESRenderComp, IBlockRenderComp, IStateInteractionHandler {

	public static final String ID = "7seg";

	SignalHandler out;
	Decoding mode = Decoding.DEC;
	String title = "";
	byte pos, color, dots;
	int value;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public int getBounds() {
		return 0xff << (4 * pos);
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		pos = (byte)Math.floor(y * 3.);
		if (pos < 0) pos = 0;
		else if (pos > 2) pos = 2;
	}

	@Override
	public void init(List<MountedPort> ports, int id, IPanel panel) {
		Orientation o = panel.getOrientation();
		ports.add(new MountedPort(panel, id << 1, SignalHandler.class, false).setLocation(0.875, 0.125 + (double)pos * 0.25, 0, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
		ports.add(new MountedPort(panel, id << 1 | 1, SignalHandler.class, true).setLocation(0.125, 0.125 + (double)pos * 0.25, 0, EnumFacing.NORTH, o).setName("port.rs_ctr.o"));
		super.init(ports, id, panel);
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.seg7);
	}

	@Override
	public Object getPortCallback() {
		return this;
	}

	@Override
	public void setPortCallback(Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null)
			out.updateSignal(mode.remainder(value));
	}

	@Override
	public void resetInput() {
		updateSignal(0);
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
	public void writeSync(PacketBuffer buf) {
		buf.writeInt(value);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void readSync(PacketBuffer buf) {
		int val = buf.readInt();
		if (val != value) {
			value = val;
			renderCache = null;
		}
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("val", value);
		nbt.setByte("type", (byte) (mode.ordinal() | pos << 4));
		nbt.setByte("cfg", (byte) (dots & 0xf | color << 4));
		nbt.setString("title", title);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		value = nbt.getInteger("val");
		int i = nbt.getByte("type");
		mode = Decoding.values()[(i & 15) % 5];
		pos = (byte) (i >> 4 & 3);
		i = nbt.getByte("cfg");
		dots = (byte) (i & 7);
		color = (byte) (i >> 4 & 15);
		title = nbt.getString("title");
		if (host != null && host.world().isRemote)
			renderCache = null;
	}

	@Override
	public AdvancedContainer getCfgContainer(EntityPlayer player) {
		return new AdvancedContainer(this, StateSynchronizer.builder().build(host.world().isRemote), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getCfgScreen(EntityPlayer player) {
		ModularGui gui = new ModularGui(getCfgContainer(player));
		GuiFrame frame = new GuiFrame(gui, 128, 43, 1)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 97);
		new TextField(frame, 112, 7, 8, 16, 20, ()-> title, (t)-> gui.sendPkt((byte)0, t)).tooltip("gui.rs_ctr.label");
		new Button(frame, 9, 9, 52, 28, 16, ()-> color, (s)-> gui.sendPkt((byte)1, (byte)s)).texture(247, 72).tooltip("gui.rs_ctr.color");
		new Button(frame, 26, 9, 65, 28, 5, ()-> mode.ordinal(), (s)-> gui.sendPkt((byte)2, (byte)s)).texture(221, 63).tooltip("gui.rs_ctr.encoder#");
		new Button(frame, 26, 9, 96, 28, 5, ()-> dots, (s)-> gui.sendPkt((byte)3, (byte)s)).texture(221, 108).tooltip("gui.rs_ctr.dot");
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

	@SideOnly(Side.CLIENT)
	IntArrayModel renderCache;

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		light = light & 0xff0000 | 0xf0;
		/*if (renderCache != null) {
			renderCache.setBrightness(light);
			renderCache.setOffset((float)x, (float)y, (float)z);
			buffer.addVertexData(renderCache.vertexData);
			return;
		}
		int vi = buffer.getVertexCount();*/
		Orientation o = host.getOrientation();
		Vec3d p = o.rotate(new Vec3d(.25, (double)(this.pos - 2) * 0.25, -.365)).addVector(x + .5, y + .5, z + .5);
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
		//renderCache = new IntArrayModel(extractData(buffer, vi, buffer.getVertexCount()), color, light);
		//renderCache.origin((float)x, (float)y, (float)z);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return null;
	}

	@Override
	public void render(List<BakedQuad> quads) {
		Orientation o = host.getOrientation();
		quads.add(new BakedQuad(texturedRect(o.rotate(new Vec3d(-.5, (double)(this.pos - 2) * 0.25, -.37)).addVector(.5, .5, .5), o.X, o.Y.scale(1./3.), getUV(blank, 0, 0), getUV(blank, 16, 16), 0xff3f3f3f, 0), -1, o.back, blank, true, DefaultVertexFormats.BLOCK));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		fr.drawString(title, (128 - fr.getStringWidth(title)) / 2, 72 - 32 * pos, 0xff000000);
	}

	static final byte[] DIGITS = {
			0b1111110, 0b0011000, 0b1101101, 0b0111101, //0, 1, 2, 3
			0b0011011, 0b0110111, 0b1110111, 0b0011100, //4, 5, 6, 7
			0b1111111, 0b0111111, 0b1011111, 0b1110011, //8, 9, A, B
			0b1100110, 0b1111001, 0b1100111, 0b1000111, //C, D, E, F
		};

	static final int[] COLORS = {
			//red      , orange     , yellow     , green      , cyan       , blue       , magenta    , white
			0xff_0000ff, 0xff_007fff, 0xff_00ffff, 0xff_00ff00, 0xff_bfbf00, 0xff_ff0000, 0xff_bf00bf, 0xff_bfbfbf,
			0xff_7f7fff, 0xff_7fbfff, 0xff_7fffff, 0xff_7fff7f, 0xff_ffff7f, 0xff_ff7f7f, 0xff_ff7fff, 0xff_ffffff,
		};

	enum Decoding {
		RAW(0, false),
		RAW_S(0, true),
		DEC(10000, false) {
			@Override
			int decode(int val) {
				if (val < 0) val = val == Integer.MIN_VALUE ? 0 : -val;
				int code = 0;
				code |= DIGITS[val % 10]; val /= 10;
				code |= DIGITS[val % 10] << 8; val /= 10;
				code |= DIGITS[val % 10] << 16; val /= 10;
				return code | DIGITS[val % 10] << 24;
			}
		}, DEC_S(0, true) {
			@Override
			int decode(int val) {
				int code = 0;
				if (val < 0) {
					val = val == Integer.MIN_VALUE ? 0 : -val;
					code = 0x2000000;
				}
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
		}, HEX(65536, false) {
			@Override
			int decode(int val) {
				int code = 0;
				for (int i = 0; i < 32; i+=8, val >>>= 4)
					code |= DIGITS[val & 15] << i;
				return code;
			}
		};

		final int div;
		final boolean sign;

		private Decoding(int div, boolean sign) {
			this.div = div;
			this.sign = sign;
		}

		int remainder(int val) {
			return div != 0 ? val / div : 0;
		}

		int decode(int val) {
			return val;
		}

	}

}
