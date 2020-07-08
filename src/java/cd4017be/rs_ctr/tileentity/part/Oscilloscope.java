package cd4017be.rs_ctr.tileentity.part;

import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import static cd4017be.lib.render.Util.getUV;
import static cd4017be.lib.render.Util.texturedRect;
import static cd4017be.rs_ctr.ClientProxy.t_osc;
import java.nio.ByteBuffer;
import java.util.List;
import org.lwjgl.opengl.GL11;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.GL_BYTE;
import static net.minecraft.client.renderer.GlStateManager.*;

/** @author CD4017BE */
public class Oscilloscope extends Module implements SignalHandler, IBlockRenderComp {

	public static final String ID = "oscilloscope";

	/** x[0..3-w], y[0..3-h], w[0..3], h[0..3] */
	protected byte pos;
	SignalHandler out;
	int[] graph = new int[0];
	int value, idx, lastSend;
	boolean paused;

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		Orientation o = panel.getOrientation();
		double x = getX() + 0.125, y = getY() + 0.375;
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, false).setLocation(x + getW(), y, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
		ports.add(new MountedPort(panel, idx << 1 | 1, SignalHandler.class, true).setLocation(x, y, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.o"));
		super.init(ports, idx, panel);
	}

	@Override
	public void updateSignal(int value) {
		if(paused || ((value ^ this.value) & 0xff000000) == 0) return;
		if (out != null)
			out.updateSignal(graph[idx]);
		if ((value & 0xff) > 100) value = value & 0xffffff00 | 100;
		if ((value >> 8 & 0xff) > 100) value = value & 0xffff00ff | 100 << 8;
		if ((value >> 16 & 0xff) > 100) value = value & 0xff00ffff | 100 << 16;
		graph[idx] = this.value = value;
		if(++idx >= graph.length) idx = 0;
		host.updateDisplay();
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setByte("pos", pos);
		nbt.setInteger("val", value);
		nbt.setByte("idx", (byte)idx);
		nbt.setIntArray("graph", graph);
		nbt.setBoolean("pause", paused);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		pos = nbt.hasKey("pos", NBT.TAG_BYTE) ? nbt.getByte("pos") : (byte)0xf0;
		int l = (pos & 0x20) != 0 ? 100 : 50;
		if (graph.length != l) graph = new int[l];
		value = nbt.getInteger("val");
		idx = (nbt.getByte("idx") & 0xff) % l;
		lastSend = idx;
		int[] arr = nbt.getIntArray("graph");
		System.arraycopy(arr, 0, graph, 0, Math.min(arr.length, l));
		paused = nbt.getBoolean("pause");
		loadCfg(nbt);
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {}

	@Override
	public int getBounds() {
		return (15 >> (3 - (pos >> 4 & 3))) * (0x1111 >> (12 - (pos >> 4 & 12))) << (pos & 15);
	}

	protected double getX() {
		return (double)(pos & 3) * .25;
	}

	protected double getY() {
		return (double)(pos >> 2 & 3) * .25;
	}

	protected double getW() {
		return (double)(pos >> 4 & 3) * .25;
	}

	protected double getH() {
		return (double)(pos >> 6 & 3) * .25;
	}

	@Override
	public Object getPortCallback() {
		return this;
	}

	@Override
	public void setPortCallback(Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null) {
			int l = graph.length;
			out.updateSignal(graph[(idx - 1 + l) % l]);
		}
	}

	@Override
	public void resetInput() {}

	@Override
	public void writeSync(PacketBuffer buf, boolean init) {
		int l = graph.length;
		if (init) {
			buf.writeByte(-1 - idx);
			for (int i = 0; i < l; i++)
				buf.writeInt(graph[i]);
		} else {
			buf.writeByte(lastSend);
			buf.writeByte(idx);
			while(lastSend != idx) {
				buf.writeInt(graph[lastSend]);
				if (++lastSend >= l) lastSend -= l;
			}
		}
		buf.writeBoolean(paused);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void readSync(PacketBuffer buf) {
		int idx = buf.readByte(), l = graph.length;
		if (idx < 0) {
			this.idx = (-1 - idx) % l;
			for (int i = 0; i < l; i++)
				graph[i] = buf.readInt();
		} else {
			int m = idx % l;
			this.idx = idx = (buf.readByte() & 0xff) % l;
			while(m != idx) {
				graph[m] = buf.readInt();
				if (++m >= l) m -= l;
			}
		}
		paused = buf.readBoolean();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		IPanel host = this.host;
		if (host == null) return;
		Orientation o = host.getOrientation();
		Vec3d p = o.rotate(new Vec3d(getX(), getY(), .505)).addVector(.5, .5, .5);
		Vec2f t0 = getUV(t_osc, .125F, .125F), t1 = getUV(t_osc, 12.625F, 12.625F);
		quads.add(new BakedQuad(texturedRect(p, o.X.scale(-.5), o.Y.scale(-.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
		if ((pos & 0x20) != 0)
			quads.add(new BakedQuad(texturedRect(p.add(o.X.scale(.5)), o.X.scale(-.5), o.Y.scale(-.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
		if ((pos & 0x80) != 0)
			quads.add(new BakedQuad(texturedRect(p.add(o.X.scale(-.5)), o.X.scale(.5), o.Y.scale(.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
		if ((pos & 0xa0) == 0xa0)
			quads.add(new BakedQuad(texturedRect(p, o.X.scale(.5), o.Y.scale(.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		pushMatrix();
		translate(.64 + (pos & 3) * 32, 128 - (pos >> 2 & 3) * 32, -.01);
		fr.drawString(paused ? "\u2223\u2223" : "\u25b6", 0, (pos & 0x80) != 0 ? -128 : -64, 0xffffffff);
		scale(1.28, (pos & 0x80) != 0 ? -1.28 : -.64, 1);
		enableBlend();
		blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		depthMask(false);
		disableLighting();
		disableTexture2D();
		setActiveTexture(OpenGlHelper.lightmapTexUnit);
		disableTexture2D();
		glEnableClientState(GL_VERTEX_ARRAY);
		glVertexPointer(2, GL_BYTE, 2, vertexArray);
		int l = graph.length;
		for (int i = 0; i < 24; i+=8) {
			color(i == 0 ? 1 : 0, i == 8 ? 1 : 0, i == 16 ? 1 : 0, 1);
			int k = 1, idx = this.idx + 1;
			for (int j = idx; j < l; j++, k+=2)
				vertexArray.put(k, (byte)(graph[j] >> i));
			for (int j = 0; j < idx; j++, k+=2)
				vertexArray.put(k, (byte)(graph[j] >> i));
			glDrawArrays(GL11.GL_LINE_STRIP, 0, l);
		}
		glDisableClientState(GL_VERTEX_ARRAY);
		depthMask(true);
		enableTexture2D();
		setActiveTexture(OpenGlHelper.defaultTexUnit);
		enableTexture2D();
		popMatrix();
		blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		if (super.onInteract(player, hit, side, aim)) return true;
		if (hit) return false;
		paused = !paused;
		host.updateDisplay();
		return true;
	}

	@Override
	public AdvancedContainer getCfgContainer(EntityPlayer player) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getCfgScreen(EntityPlayer player) {
		return null;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		int m = stack.getMetadata();
		pos = (byte)(0x50
			| ((m & 1) == 0 ? (int)Math.floor(x * 3F) & 3 : 0x20)
			| ((m & 2) == 0 ? (int)Math.floor(y * 3F) << 2 & 12 : 0x80)
		);
		graph = new int[(m & 1) != 0 ? 100 : 50];
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.oscilloscope);
	}

	private static final ByteBuffer vertexArray;
	static {
		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			vertexArray = GLAllocation.createDirectByteBuffer(200);
			for (int i = 0; i < 100; i++)
				vertexArray.put((byte)i).put((byte)0);
			vertexArray.flip();
		} else vertexArray = null;
	}
	@Override
	public Object getState(int id) {
		return id == 0 ? value : graph[idx];
	}

}
