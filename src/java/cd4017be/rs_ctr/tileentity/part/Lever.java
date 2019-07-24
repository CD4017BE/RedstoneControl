package cd4017be.rs_ctr.tileentity.part;

import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;

import static cd4017be.rs_ctr.render.PortRenderer.PORT_RENDER;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
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
public class Lever extends SignalModule {

	public static final String ID = "lever";

	SignalHandler out;
	int onVal = 65535, offVal = 0;
	byte color;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		pos = (byte) ((int)Math.floor(x * 4F) & 3 | (int)Math.floor(y * 4F) << 2 & 12);
		color = (byte) (stack.getMetadata() != 0 ? 0 : -1);
		if (color < 0) title = "ON";
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.lever, 1, color >= 0 ? 1 : 0);
	}

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, true).setLocation(getX() + .125, getY() + .125, 0, EnumFacing.NORTH, panel.getOrientation()).setName("port.rs_ctr.o"));
		super.init(ports, idx, panel);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		if (super.onInteract(player, hit, side, aim)) return true;
		if (hit) return false;
		value = ~value;
		if (out != null)
			out.updateSignal(value != 0 ? onVal : offVal);
		host.updateDisplay();
		return true;
	}

	@Override
	public void setPortCallback(Object callback) {
		if (callback instanceof SignalHandler)
			(out = (SignalHandler)callback).updateSignal(value != 0 ? onVal : offVal);
		else out = null;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("on", onVal);
		nbt.setInteger("off", offVal);
		nbt.setByte("color", color);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		onVal = nbt.getInteger("on");
		offVal = nbt.getInteger("off");
		color = nbt.getByte("color");
		super.deserializeNBT(nbt);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(Orientation o, double x, double y, double z, int light, BufferBuilder buffer) {
		Vec3d p = o.X.scale(getX()).add(o.Y.scale(getY()));
		if (color < 0)
			(renderCache = PORT_RENDER.getModel(value != 0 ? "_lever.on" : "_lever.off").rotated(o)).origin(-(float)p.x, -(float)p.y, -(float)p.z);
		else {
			int c = color & 15;
			if (value != 0) p = p.add(o.Z.scale(-.03125));
			else c |= 16;
			(renderCache = PORT_RENDER.getModel("_lever.btn").rotated(o)).origin(-(float)p.x, -(float)p.y, -(float)p.z).setColor(_7Segment.COLORS[c]);
			renderCache.setBrightness(brightness(light));
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected int brightness(int light) {
		return color >= 0 && value != 0 ? light & 0xff0000 | 0xf0 : light;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		if (title.isEmpty()) return;
		if (color < 0) {
			super.drawText(fr);
			return;
		}
		float dz = value != 0 ? 0.03125F : 0.0625F;
		GlStateManager.translate(0, 0, -dz);
		int x = (pos & 3) * 32, y = 96 - (pos >> 2 & 3) * 32;
		List<String> lines = fr.listFormattedStringToWidth(title, 26);
		y += (32 - lines.size() * 8) / 2;
		for (String s : lines) {
			fr.drawString(s, x + (32 - fr.getStringWidth(s)) / 2, y, 0xff000000);
			y += 8;
		}
		GlStateManager.translate(0, 0, dz);
	}

	@Override
	protected GuiFrame initGuiFrame(ModularGui gui) {
		GuiFrame frame = new GuiFrame(gui, 128, 53, 4)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 140);
		new TextField(frame, 112, 7, 8, 16, color < 0 ? 6 : 16, ()-> title, (t)-> gui.sendPkt((byte)0, t)).allowFormat().tooltip("gui.rs_ctr.label");
		new TextField(frame, 64, 7, 56, 29, 12, ()-> Integer.toString(onVal), (t)-> {
			try {gui.sendPkt((byte)2, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.v_on");
		new TextField(frame, 64, 7, 56, 38, 12, ()-> Integer.toString(offVal), (t)-> {
			try {gui.sendPkt((byte)1, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.v_off");
		if (color >= 0)
			new Button(frame, 9, 9, 7, 28, 16, ()-> color, (s)-> gui.sendPkt((byte)3, (byte)s)).texture(247, 72).tooltip("gui.rs_ctr.color");
		return frame;
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		switch(pkt.readByte()) {
		case 0: title = pkt.readString(32); break;
		case 1:
			onVal = pkt.readInt();
			if (out != null && value != 0)
				out.updateSignal(onVal);
			break;
		case 2:
			offVal = pkt.readInt();
			if (out != null && value == 0)
				out.updateSignal(offVal);
			break;
		case 3:
			if (color < 0) return;
			color = (byte) (pkt.readByte() & 15);
			break;
		default: return;
		}
		host.markDirty(BaseTileEntity.SYNC);
	}

}
