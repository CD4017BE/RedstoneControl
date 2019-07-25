package cd4017be.rs_ctr.tileentity.part;

import static cd4017be.rs_ctr.render.PortRenderer.PORT_RENDER;

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
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
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
public class Lamp extends SignalModule implements SignalHandler {

	public static final String ID = "lamp";

	byte color;
	int thr;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		pos = (byte) ((int)Math.floor(x * 4F) & 3 | (int)Math.floor(y * 4F) << 2 & 12);
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.lamp);
	}

	@Override
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		ports.add(new MountedPort(panel, idx << 1, SignalHandler.class, false).setLocation(getX() + .125, getY() + .125, 0, EnumFacing.NORTH, panel.getOrientation()).setName("port.rs_ctr.i"));
		super.init(ports, idx, panel);
	}

	@Override
	public void updateSignal(int value) {
		if (value > thr ^ this.value > thr)
			host.updateDisplay();
		this.value = value;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setByte("color", color);
		nbt.setInteger("thr", thr);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		color = nbt.getByte("color");
		thr = nbt.getInteger("thr");
		super.deserializeNBT(nbt);
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected boolean refreshFTESR(Orientation o, double x, double y, double z, int light, BufferBuilder buffer) {
		Vec3d p = o.X.scale(getX()).add(o.Y.scale(getY())).add(o.Z.scale(-.046875));
		int c = color & 15;
		if (value <= thr) c |= 16;
		(renderCache = PORT_RENDER.getModel("_lever.btn").rotated(o)).origin(-(float)p.x, -(float)p.y, -(float)p.z).setColor(_7Segment.COLORS[c]);
		renderCache.setBrightness(brightness(light));
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected int brightness(int light) {
		return value > thr ? light & 0xff0000 | 0xf0 : light;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		if (title.isEmpty()) return;
		float dz = 0.015625F;
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
		GuiFrame frame = new GuiFrame(gui, 80, 53, 3)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 0, 151);
		new TextField(frame, 64, 7, 8, 16, 16, ()-> title, (t)-> gui.sendPkt((byte)0, t)).allowFormat().tooltip("gui.rs_ctr.label");
		new TextField(frame, 64, 7, 8, 29, 12, ()-> Integer.toString(thr), (t)-> {
			try {gui.sendPkt((byte)1, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.thr");
		new Button(frame, 9, 9, 7, 37, 16, ()-> color & 15, (s)-> gui.sendPkt((byte)2, (byte)s)).texture(247, 72).tooltip("gui.rs_ctr.color");
		return frame;
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		switch(pkt.readByte()) {
		case 0: title = pkt.readString(32); break;
		case 1: thr = pkt.readInt(); break;
		case 2: color = pkt.readByte(); break;
		default: return;
		}
		host.markDirty(BaseTileEntity.SYNC);
	}

}
