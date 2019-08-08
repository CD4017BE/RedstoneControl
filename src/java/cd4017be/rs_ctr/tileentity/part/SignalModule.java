package cd4017be.rs_ctr.tileentity.part;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public abstract class SignalModule extends Module implements IStateInteractionHandler, ITESRenderComp {

	/** x[0..3-w], y[0..3-h], w[0..3], h[0..3] */
	protected byte pos;
	protected String title = "";
	protected int value;

	/**warning: multi-threaded access */
	@SideOnly(Side.CLIENT)
	IntArrayModel renderCache;

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setByte("pos", pos);
		nbt.setInteger("val", value);
		nbt.setString("title", title);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		pos = nbt.getByte("pos");
		value = nbt.getInteger("val");
		loadCfg(nbt);
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {
		title = nbt.getString("title");
		if (host != null && host.world().isRemote)
			renderCache = null;
	}

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

	@Override
	public Object getPortCallback() {
		return this instanceof SignalHandler ? this : null;
	}

	@Override
	public void setPortCallback(Object callback) {
	}

	@Override
	public void resetInput() {
		if (this instanceof SignalHandler)
			((SignalHandler)this).updateSignal(0);
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
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		int x = (pos & 3) * 32, y = (3 - (pos >> 2 & 3) - (pos >> 6 & 3)) * 32, w = (pos >> 4 & 3) * 32 + 32;
		if ((pos >> 6 & 3) != 0) y += 8;
		fr.drawString(title, x + (w - fr.getStringWidth(title)) / 2, y, 0xff000000);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		light = host.frontLight();
		IntArrayModel m = renderCache;
		if (m == null && (refreshFTESR(host.getOrientation(), x, y, z, light, buffer) || (m = renderCache) == null)) return;
		m.setBrightness(brightness(light));
		buffer.addVertexData(m.translated((float)x, (float)y, (float)z).vertexData);
	}

	@SideOnly(Side.CLIENT)
	protected abstract boolean refreshFTESR(Orientation o, double x, double y, double z, int light, BufferBuilder buffer);

	@SideOnly(Side.CLIENT)
	protected int brightness(int light) {
		return light;
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return null;
	}

	@Override
	public AdvancedContainer getCfgContainer(EntityPlayer player) {
		return new AdvancedContainer(this, StateSynchronizer.builder().build(host.world().isRemote), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getCfgScreen(EntityPlayer player) {
		ModularGui gui = new ModularGui(getCfgContainer(player));
		gui.compGroup = initGuiFrame(gui);
		return gui;
	}

	@SideOnly(Side.CLIENT)
	protected abstract GuiFrame initGuiFrame(ModularGui gui);

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

}
