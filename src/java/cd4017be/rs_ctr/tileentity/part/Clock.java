package cd4017be.rs_ctr.tileentity.part;

import static cd4017be.lib.render.Util.extractData;
import static cd4017be.lib.render.Util.getUV;
import static cd4017be.lib.render.Util.texturedRect;
import static cd4017be.rs_ctr.ClientProxy.*;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.ITickReceiver;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
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
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author cd4017be */
public class Clock extends SignalModule
implements SignalHandler, ITickReceiver, IBlockRenderComp {

	public static final String ID = "clock";

	SignalHandler out = SignalHandler.NOP;
	int interval = -20;
	{
		title = "Clock";
		value = Integer.MAX_VALUE;
	}

	@Override
	public void updateSignal(int value) {
		if(interval < 0) {
			if(value == 0) return;
			if(
				this.value == Integer.MAX_VALUE
				|| this.value == Integer.MIN_VALUE
			) {
				this.value = this.value > 0 ? 0 : interval;
				TickRegistry.instance.add(this);
			}
		} else if(value != 0) return;
		interval = -interval;
		host.updateDisplay();
	}

	@Override
	public boolean tick() {
		if(host == null) return false;
		if(interval <= 0) {
			value = value >= 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			return false;
		}
		int v = value + 1;
		if(v >= interval) {
			v = -interval;
			out.updateSignal(0xffff);
			host.updateDisplay();
		} else if(v == 0) {
			out.updateSignal(0);
			host.updateDisplay();
		}
		value = v;
		return true;
	}

	@Override
	public void setPortCallback(Object callback) {
		if(callback instanceof SignalHandler) {
			SignalHandler h = (SignalHandler)callback;
			h.updateSignal(value >= 0 ? 0 : 0xffff);
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
		int d = increment(aim, host.getOrientation());
		int i = Math.abs(interval) + d;
		if(i < 1) i = 1;
		else if(i > 1200) i = 1200;
		interval = interval < 0 ? -i : i;
		host.updateDisplay();
		return true;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		IPanel host = this.host;
		if(host == null) return null;
		Orientation o = host.getOrientation();
		int val = increment(aim, o);
		return val != 0 ? Pair.of(aim, String.format("%+.2f", val / 20F)) : null;
	}

	private int increment(Vec3d aim, Orientation o) {
		aim = aim.subtract(.5, .5, .5);
		double d = aim.dotProduct(o.Y) + .5 - getY();
		if (d < .0625 || d > .1875) return 0;
		double dx = aim.dotProduct(o.X) + .5 - getX();
		return dx < .25 ? dx < .125 ? -20 : -1 : dx < .375 ? 1 : 20;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setShort("int", (short)interval);
		return nbt;
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {
		int i = nbt.getShort("int");
		interval = interval > 0 ^ i > 0 ? -i : i;
		super.loadCfg(nbt);
	}

	@Override
	public void onLoad(IPanel host) {
		if(interval > 0 && !host.world().isRemote && this.host == null) {
			if(value == Integer.MIN_VALUE) value = -interval;
			else if(value == Integer.MAX_VALUE) value = 0;
			TickRegistry.instance.add(this);
		}
		super.onLoad(host);
	}

	@Override
	public void writeSync(PacketBuffer buf, boolean init) {
		buf.writeByte(value < 0 ? -1 : 0);
		buf.writeShort(interval);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void readSync(PacketBuffer buf) {
		int val = buf.readByte(), interv = buf.readShort();
		if(value != val || interval != interv) {
			value = val;
			interval = interv;
			renderCache = null;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(
		Orientation o, double x, double y, double z, int light,
		BufferBuilder buffer
	) {
		light = brightness(light);
		int vi = buffer.getVertexCount();
		Vec3d p = o.rotate(new Vec3d(getX() - .5 + .0625, getY() - .5, .51))
		.addVector(x + .5, y + .5, z + .5);
		Vec3d dx = o.X.scale(0.0625), dy = o.Y.scale(0.0625);
		Vec2f t0 = getUV(t_blank, 0, 0), t1 = getUV(t_blank, 16, 16);
		buffer.addVertexData(
			texturedRect(
				p, dx, dy, t0, t1, value < 0 ? 0xff0000ff : 0xff00003f, light
			)
		);
		buffer.addVertexData(
			texturedRect(
				p.add(dx.scale(5)), dx, dy, t0, t1, interval > 0 ? 0xff00ff00 : 0xff003f00, light
			)
		);
		IntArrayModel m = new IntArrayModel(
			extractData(buffer, vi, buffer.getVertexCount()), -1, light
		);
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
		if(host == null) return;
		Orientation o = host.getOrientation();
		quads.add(
			new BakedQuad(
				texturedRect(
					o.rotate(new Vec3d(getX() - .4375, getY() - .5, .505)).addVector(.5, .5, .5),
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
		String s = TooltipUtil.format("port.rs_ctr.clock", Math.abs(interval) / 20F);
		GlStateManager.translate(0, 0, -.005);
		fr.drawString(
			s, x + (w - fr.getStringWidth(s)) / 2, y + 12, 0xffc0c000
		);
		GlStateManager.translate(0, 0, .005);
	}

	@Override
	protected GuiFrame initGuiFrame(ModularGui gui) {
		GuiFrame frame = new GuiFrame(gui, 80, 31, 1)
		.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
		.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 0, 0);
		new TextField(
			frame, 112, 7, 8, 16, 20, () -> title, (t) -> gui.sendPkt(t)
		).allowFormat().tooltip("gui.rs_ctr.label");
		return frame;
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender)
	throws Exception {
		title = pkt.readString(32);
		host.markDirty(BaseTileEntity.SYNC);
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		if(stack.hasTagCompound())
			interval = -stack.getTagCompound().getInteger("int");
		pos = (byte)(16 | (int)Math.floor(x * 3.) & 3
		| (int)Math.floor(y * 4.) << 2 & 12);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public ItemStack onRemove() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("int", Math.abs(interval));
		ItemStack stack = new ItemStack(Objects.clock);
		stack.setTagCompound(nbt);
		return stack;
	}

}
