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

	SignalHandler out;
	int[] graph = new int[100];
	int value, idx;

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		Orientation o = panel.getOrientation();
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, false).setLocation(0.875, 0.5, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.i"));
		ports.add(new MountedPort(panel, idx << 1 | 1, SignalHandler.class, true).setLocation(0.125, 0.5, 0.75, EnumFacing.NORTH, o).setName("port.rs_ctr.o"));
		super.init(ports, idx, panel);
	}

	@Override
	public void updateSignal(int value) {
		if(((value ^ this.value) & 0xff000000) == 0) return;
		if (out != null)
			out.updateSignal(graph[idx]);
		if ((value & 0xff) > 100) value = value & 0xffffff00 | 100;
		if ((value >> 8 & 0xff) > 100) value = value & 0xffff00ff | 100 << 8;
		if ((value >> 16 & 0xff) > 100) value = value & 0xff00ffff | 100 << 16;
		graph[idx] = this.value = value;
		if(++idx >= 100) idx = 0;
		host.updateDisplay();
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("val", value);
		nbt.setByte("idx", (byte)idx);
		nbt.setIntArray("graph", graph);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		value = nbt.getInteger("val");
		idx = (nbt.getByte("idx") & 0xff) % 100;
		int[] arr = nbt.getIntArray("graph");
		System.arraycopy(arr, 0, graph, 0, Math.min(arr.length, 100));
		loadCfg(nbt);
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {}

	@Override
	public int getBounds() {
		return 0xffff;
	}

	@Override
	public Object getPortCallback() {
		return this;
	}

	@Override
	public void setPortCallback(Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null) out.updateSignal(graph[(idx + 99) % 100]);
	}

	@Override
	public void resetInput() {}

	@Override
	public void writeSync(PacketBuffer buf, boolean init) {
		if (init) {
			buf.writeByte(-1 - idx);
			for (int i = 0; i < 100; i++)
				buf.writeInt(graph[i]);
		} else {
			buf.writeByte(idx);
			buf.writeInt(value);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void readSync(PacketBuffer buf) {
		if ((idx = buf.readByte()) < 0) {
			idx = -1 - idx;
			for (int i = 0; i < 100; i++)
				graph[i] = buf.readInt();
		} else graph[idx % 100] = buf.readInt();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		IPanel host = this.host;
		if (host == null) return;
		Orientation o = host.getOrientation();
		Vec3d p = o.rotate(new Vec3d(0, 0, .505)).addVector(.5, .5, .5);
		Vec2f t0 = getUV(t_osc, .125F, .125F), t1 = getUV(t_osc, 12.625F, 12.625F);
		quads.add(new BakedQuad(texturedRect(p, o.X.scale(.5), o.Y.scale(.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
		quads.add(new BakedQuad(texturedRect(p, o.X.scale(-.5), o.Y.scale(-.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
		quads.add(new BakedQuad(texturedRect(p.add(o.X.scale(-.5)), o.X.scale(.5), o.Y.scale(.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
		quads.add(new BakedQuad(texturedRect(p.add(o.X.scale(.5)), o.X.scale(-.5), o.Y.scale(-.5), t0, t1, 0xff3f3f3f, 0), -1, o.back, t_osc, true, DefaultVertexFormats.BLOCK));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		pushMatrix();
		enableBlend();
		blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		depthMask(false);
		disableLighting();
		disableTexture2D();
		setActiveTexture(OpenGlHelper.lightmapTexUnit);
		disableTexture2D();
		scale(1.28, -1.28, 1);
		translate(.5, -100, -.01);
		glEnableClientState(GL_VERTEX_ARRAY);
		glVertexPointer(2, GL_BYTE, 2, vertexArray);
		for (int i = 0; i < 24; i+=8) {
			color(i == 0 ? 1 : 0, i == 8 ? 1 : 0, i == 16 ? 1 : 0, 1);
			int k = 1;
			for (int j = idx + 1; j < 100; j++, k+=2)
				vertexArray.put(k, (byte)(graph[j] >> i));
			for (int j = 0; j <= idx; j++, k+=2)
				vertexArray.put(k, (byte)(graph[j] >> i));
			glDrawArrays(GL11.GL_LINE_STRIP, 0, 100);
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
	public AdvancedContainer getCfgContainer(EntityPlayer player) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getCfgScreen(EntityPlayer player) {
		return null;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {}

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

}
