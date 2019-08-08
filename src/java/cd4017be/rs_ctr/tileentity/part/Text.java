package cd4017be.rs_ctr.tileentity.part;

import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.Gui.comp.Slider;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public class Text extends Module implements IStateInteractionHandler {

	public static final String ID = "text";
	final String[] lines = new String[] {"", "Text", "", ""};
	/** x[0..4-w], y[0..3], w[0..3] */
	byte pos;
	byte align = 0;

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setByte("pos", pos);
		for (int i = 0; i < lines.length; i++)
			nbt.setString("l" + i, lines[i]);
		nbt.setByte("align", align);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		pos = nbt.getByte("pos");
		loadCfg(nbt);
	}

	@Override
	protected void loadCfg(NBTTagCompound nbt) {
		for (int i = 0; i < lines.length; i++)
			lines[i] = nbt.getString("l" + i);
		align = nbt.getByte("align");
	}

	@Override
	public void onPlaced(ItemStack stack, float x, float y) {
		int w = stack.getMetadata() - 1 & 3;
		pos = (byte) (w << 4 | (int)Math.floor(x * (double)(4 - w)) & 3 | (int)Math.floor(y * 4.) << 2 & 12);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public int getBounds() {
		return 15 >> (3 - (pos >> 4 & 3)) << (pos & 15);
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.text, 1, (pos >> 4 & 3) + 1);
	}

	@Override
	public Object getPortCallback() {
		return null;
	}

	@Override
	public void setPortCallback(Object callback) {
	}

	@Override
	public void resetInput() {
	}

	@Override
	public void writeSync(PacketBuffer buf) {
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void readSync(PacketBuffer buf) {
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
		int x = (pos & 3) * 32, y = 96 - (pos >> 2 & 3) * 32, w = (pos >> 4 & 3) * 32 + 32;
		for (String s : lines) {
			fr.drawString(s, x + (w - fr.getStringWidth(s)) * (align + 1) / 2, y, 0xff000000);
			y += 8;
		}
	}

	@Override
	public AdvancedContainer getCfgContainer(EntityPlayer player) {
		return new AdvancedContainer(this, StateSynchronizer.builder().build(host.world().isRemote), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getCfgScreen(EntityPlayer player) {
		ModularGui gui = new ModularGui(getCfgContainer(player));
		GuiFrame frame = new GuiFrame(gui, 144, 62, 5)
				.title("gui.rs_ctr.dsp_cfg.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 193);
		int l = (pos >> 4 & 3) * 6 + 8;
		for (int i = 0; i < lines.length; i++) {
			byte i_ = (byte)i;
			new TextField(frame, 128, 7, 8, 16 + i * 9, l, ()-> lines[i_], (t)-> gui.sendPkt(i_, t)).allowFormat().tooltip("gui.rs_ctr.text");
		}
		new Slider(frame, 32, 3, 128, 8, 52, 224, 253, true, ()-> (double)(align + 1) * .5, (v)-> align = (byte)(Math.rint(v * 2.0) - 1), ()-> gui.sendPkt((byte)4, align)).tooltip("gui.rs_ctr.align");
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
		int i = pkt.readUnsignedByte();
		if (i >= lines.length) {
			align = pkt.readByte();
			return;
		}
		lines[i] = pkt.readString(32);
		host.markDirty(BaseTileEntity.SYNC);
	}

}
