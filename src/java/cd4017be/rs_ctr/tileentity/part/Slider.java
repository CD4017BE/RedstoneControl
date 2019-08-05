package cd4017be.rs_ctr.tileentity.part;

import static cd4017be.lib.render.Util.getUV;
import static cd4017be.lib.render.Util.texturedRect;
import static cd4017be.rs_ctr.render.PanelRenderer.blank;
import static cd4017be.rs_ctr.render.PanelRenderer.dial;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
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
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public class Slider extends SignalModule implements IBlockRenderComp {

	public static final String ID = "slider";

	SignalHandler out;
	int max = 15, min = 0;

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, true).setLocation(0.125, 0.125 + getY(), .75, EnumFacing.NORTH, panel.getOrientation()).setName("port.rs_ctr.o"));
		super.init(ports, idx, panel);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("min", min);
		nbt.setInteger("max", max);
		return nbt;
	}

	@Override
	public void loadCfg(NBTTagCompound nbt) {
		min = nbt.getInteger("min");
		max = nbt.getInteger("max");
		super.loadCfg(nbt);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		if (super.onInteract(player, hit, side, aim)) return true;
		value = value(aim);
		if (out != null)
			out.updateSignal(value);
		host.updateDisplay();
		return true;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		int val = value(aim);
		Orientation o = host.getOrientation();
		aim = aim.add(o.Y.scale(-.375 + getY() - aim.subtract(.5, .5, .5).dotProduct(o.Y)));
		return Pair.of(aim, String.format("%d", val));
	}

	private int value(Vec3d aim) {
		aim = aim.subtract(.5, .5, .5);
		Orientation o = host.getOrientation();
		double v = (aim.dotProduct(o.X) + .375) / .75;
		if (aim.dotProduct(o.Y) + .25 >= getY()) {
			double f = 1D / (v - .5);
			v = ((double)value - (double)min) / ((double)max - (double)min);
			if (f < 0) v -= Math.exp(f);
			else v += Math.exp(-f);
		}
		if (v < 0) v = 0;
		else if (v > 1) v = 1;
		return (int)Math.round((double)min + ((double)max - (double)min) * v);
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		pos = (byte) ((int)Math.floor(y * 3.) << 2 & 12 | 0x70);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.slider);
	}

	@Override
	public void setPortCallback(Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null)
			out.updateSignal(value);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected GuiFrame initGuiFrame(ModularGui gui) {
		GuiFrame frame = new GuiFrame(gui, 128, 53, 3)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 140);
		new TextField(frame, 112, 7, 8, 16, 20, ()-> title, (t)-> gui.sendPkt((byte)0, t)).allowFormat().tooltip("gui.rs_ctr.label");
		new TextField(frame, 64, 7, 56, 29, 12, ()-> Integer.toString(max), (t)-> {
			try {gui.sendPkt((byte)2, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.max");
		new TextField(frame, 64, 7, 56, 38, 12, ()-> Integer.toString(min), (t)-> {
			try {gui.sendPkt((byte)1, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.min");
		return frame;
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		switch(pkt.readByte()) {
		case 0: title = pkt.readString(32); break;
		case 1: min = pkt.readInt(); break;
		case 2: max = pkt.readInt(); break;
		default: return;
		}
		host.markDirty(BaseTileEntity.REDRAW);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(Orientation o, double x, double y, double z, int light, BufferBuilder buffer) {
		double f = ((double)value - (double)min) / ((double)max - (double)min);
		renderCache = new IntArrayModel(texturedRect(o.rotate(new Vec3d(-.421875 + f * .75, getY() - 0.4375, .51)).addVector(.5, .5, .5), o.X.scale(.09375), o.Y.scale(.25), getUV(dial, 2, 14), getUV(dial, 2.75F, 12), -1, light), -1, light);
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		Orientation o = host.getOrientation();
		double y = getY() - 0.5;
		quads.add(new BakedQuad(texturedRect(o.rotate(new Vec3d(-.375, y + .125, .505)).addVector(.5, .5, .5), o.X.scale(.75), o.Y.scale(.0625), getUV(blank, 0, 0), getUV(blank, 16, 16), 0xff3f3f3f, 0), -1, o.back, blank, true, DefaultVertexFormats.BLOCK));
		quads.add(new BakedQuad(texturedRect(o.rotate(new Vec3d(-.3875, y + .25, .505)).addVector(.5, .5, .5), o.X.scale(.775), o.Y.scale(.125), getUV(dial, 0, 16), getUV(dial, 7.75F, 14), 0xff000000, 0), -1, o.back, dial, true, DefaultVertexFormats.BLOCK));
	}

}
