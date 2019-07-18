package cd4017be.rs_ctr.tileentity;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.render.PanelRenderer.Layout;
import cd4017be.rs_ctr.render.ISpecialRenderComp;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
 * @author CD4017BE
 *
 */
public class AnalogDisplay extends WallMountGate implements SignalHandler, ITESRenderComp, IBlockRenderComp, ISpecialRenderComp, IInteractiveComponent, IGuiHandlerTile, IStateInteractionHandler {

	byte type;
	int max = 15, min = 0, exp = 0;
	String title = "", unit = "";
	int value;
	{
		ports = new MountedPort[] {new MountedPort(this, 0, SignalHandler.class, false).setLocation(0.5, 0.5, 0, EnumFacing.NORTH).setName("port.rs_ctr.i")};
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
	}

	@Override
	protected void resetPin(int pin) {
		updateSignal(0);
	}

	@Override
	public void updateSignal(int value) {
		if (this.value == value) return;
		this.value = value;
		markDirty(SYNC);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		nbt.setInteger("val", value);
		nbt.setInteger("max", max);
		nbt.setInteger("min", min);
		nbt.setByte("exp", (byte)exp);
		nbt.setByte("type", type);
		nbt.setString("title", title);
		nbt.setString("unit", unit);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		value = nbt.getInteger("val");
		max = nbt.getInteger("max");
		min = nbt.getInteger("min");
		exp = nbt.getByte("exp");
		type = nbt.getByte("type");
		title = nbt.getString("title");
		unit = nbt.getString("unit");
		super.loadState(nbt, mode);
		if (mode == SYNC && world.isRemote)
			model = null;
	}

	@Override
	protected void orient(Orientation o) {
		super.orient(o);
		if (world != null && world.isRemote) model = null;
		btnPos = o.rotate(new Vec3d(0, -0.4375, -0.375)).addVector(0.5, 0.5, 0.5);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(this);
	}

	Vec3d btnPos = Vec3d.ZERO;
	@SideOnly(Side.CLIENT)
	IntArrayModel model;

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (model == null)
			model = Layout.of(type).getPointer(((double)value - (double)min) / ((double)max - (double)min), light).rotated(o);
		model.setBrightness(light);
		buffer.addVertexData(model.translated((float)x, (float)y, (float)z).vertexData);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		Layout.of(type).drawScale(quads, o, min, max, exp);//0xff000000
	}

	static final char[] PREFIX = {'p', 'n', '\u03bc', 'm', ' ', 'k', 'M', 'G', 'T', 'P', 'E'};

	@Override
	@SideOnly(Side.CLIENT)
	public void renderSpecial(double x, double y, double z, float t, FontRenderer fr) {
		int exp = this.exp;
		for (int mag = Math.max(Math.abs(min), Math.abs(max)); mag >= 10; mag /= 10) exp++;
		exp = (exp + 12) / 3;
		String unit = exp != 4 && exp >= 0 && exp < PREFIX.length ? PREFIX[exp] + this.unit : this.unit;
		if (title.isEmpty() && unit.isEmpty()) return;
		GlStateManager.pushMatrix();
		Util.moveAndOrientToBlock(x, y, z, o);
		Layout.of(type).drawText(fr, title, unit);
		GlStateManager.popMatrix();
	}

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		return IInteractiveComponent.rayTraceFlat(start, dir, btnPos, o.back, 0.0625F, 0.0625F);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		if (!world.isRemote) GuiNetworkHandler.openBlockGui(player, pos, 0);
		return true;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		return Pair.of(btnPos, TooltipUtil.translate("port.rs_ctr.cfg"));
	}

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		return new AdvancedContainer(this, StateSynchronizer.builder().build(world.isRemote), player);
	}

	@Override
	public ModularGui getGuiScreen(EntityPlayer player, int id) {
		ModularGui gui = new ModularGui(getContainer(player, id));
		GuiFrame frame = new GuiFrame(gui, 128, 66, 7)
				.title("gui.rs_ctr.analog_dsp.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 31);
		new TextField(frame, 112, 7, 8, 16, 20, ()-> title, (t)-> gui.sendPkt(A_TITLE, t)).tooltip("gui.rs_ctr.label");
		new TextField(frame, 75, 7, 45, 29, 12, ()-> unit, (t)-> gui.sendPkt(A_UNIT, t)).tooltip("gui.rs_ctr.unit");
		new TextField(frame, 64, 7, 56, 42, 12, ()-> Integer.toString(max), (t)-> {
			try {gui.sendPkt(A_MAX, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.max");
		new TextField(frame, 64, 7, 56, 51, 12, ()-> Integer.toString(min), (t)-> {
			try {gui.sendPkt(A_MIN, Integer.parseInt(t));}
			catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.min");
		new Button(frame, 18, 18, 7, 41, 2, ()-> type, (s)-> gui.sendPkt(A_TYPE, (byte)s)).texture(229, 0).tooltip("gui.rs_ctr.style");
		new Button(frame, 20, 9, 7, 28, 3, ()-> Math.floorMod(exp, 3), (s)-> gui.sendPkt(A_EXP, (byte)(s + Math.floorDiv(exp, 3) * 3))).texture(227, 36).tooltip("gui.rs_ctr.uscale");
		new Button(frame, 9, 9, 31, 28, 8, ()-> (exp + 12) / 3, (s)-> gui.sendPkt(A_EXP, (byte)(s * 3 - 12 + Math.floorMod(exp, 3)))).texture(247, 0).tooltip("gui.rs_ctr.uscale");
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
		return !unloaded && !player.isDead && player.getDistanceSqToCenter(pos) < 256;
	}

	static final byte A_MIN = 0, A_MAX = 1, A_EXP = 2, A_TYPE = 3, A_UNIT = 4, A_TITLE = 5;

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		switch(pkt.readByte()) {
		case A_MIN: min = pkt.readInt(); break;
		case A_MAX: max = pkt.readInt(); break;
		case A_EXP: exp = pkt.readByte(); break;
		case A_TYPE: type = pkt.readByte(); break;
		case A_UNIT: unit = pkt.readString(32); break;
		case A_TITLE: title = pkt.readString(32); break;
		default: return;
		}
		markDirty(REDRAW);
	}

}
