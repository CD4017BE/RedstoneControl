package cd4017be.rs_ctr.item;

import java.util.function.Supplier;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.Connector.IConnectorItem;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.ItemInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import static cd4017be.lib.network.GuiNetworkHandler.*;
import cd4017be.lib.network.IGuiHandlerItem;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer.Builder;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.port.Constant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class ItemConstantPlug extends ItemPlug implements IConnectorItem, IGuiHandlerItem {

	/**
	 * @param id
	 */
	public ItemConstantPlug(String id) {
		super(id, SignalHandler.class, false);
	}

	@Override
	protected Connector create(MountedPort port, ItemStack stack, EntityPlayer player) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) return null;
		Constant c = new Constant(port);
		c.deserializeNBT(nbt);
		return c;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		String s = super.getItemStackDisplayName(item);
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt != null) s += " \u00a79" + toString(nbt.getInteger("val"), nbt.getByte("dsp"));
		return s;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (!player.isSneaking())
			openHeldItemGui(player, hand, 0, 0, 0);
		else if (stack.hasTagCompound()) {
			stack = stack.copy();
			NBTTagCompound nbt = stack.getTagCompound();
			if (nbt.getInteger("val") == 0)
				stack.setTagCompound(null);
			else nbt.setByte("dsp", (byte)((nbt.getByte("dsp") + 1) % 3));
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public AdvancedContainer getContainer(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z) {
		return new StateInteractionHandler(slot).createContainer(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getGuiScreen(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z) {
		StateInteractionHandler state = new StateInteractionHandler(slot);
		ModularGui gui = new ModularGui(state.createContainer(player));
		GuiFrame frame = new GuiFrame(gui, 80, 31, 1)
				.title("gui.rs_ctr.constant.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 0, 0);
		frame.add(new TextField(frame, 64, 7, 8, 16, 12, state, (t)-> {
			try {
				if (t.isEmpty()) return;
				t = t.toLowerCase();
				char c = t.charAt(0);
				if (c == 'x') gui.sendPkt(Integer.parseInt(t.substring(1), 16), (byte)1);
				else if (c == 'b') gui.sendPkt(Integer.parseInt(t.substring(1), 2), (byte)2);
				else gui.sendPkt(Integer.parseInt(t), (byte)0);
			} catch (NumberFormatException e) {}
		}));
		gui.compGroup = frame;
		return gui;
	}

	static class StateInteractionHandler extends ItemInteractionHandler implements Supplier<String> {

		int value;
		byte dsp;

		public StateInteractionHandler(int slot) {
			super(Objects.constant, slot);
		}

		@Override
		protected void initSync(Builder sb) {
			sb.addFix(4, 1);
		}

		@Override
		public void writeState(StateSyncServer state, AdvancedContainer cont) {
			NBTTagCompound nbt = getNBT(cont.player);
			state.buffer.writeInt(nbt.getInteger("val")).writeByte(nbt.getByte("dsp"));
			state.endFixed();
		}

		@Override
		public void readState(StateSyncClient state, AdvancedContainer cont) {
			value = state.get(value);
			dsp = (byte)state.get(dsp);
		}

		@Override
		public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
			NBTTagCompound nbt = getNBT(sender);
			nbt.setInteger("val", pkt.readInt());
			nbt.setByte("dsp", pkt.readByte());
		}

		@Override
		public String get() {
			return ItemConstantPlug.toString(value, dsp);
		}

	}

	public static String toString(int value, byte dsp) {
		switch(dsp) {
		case 0:
			return Integer.toString(value);
		case 1:
			return 'x' + Integer.toHexString(value);
		default:
			return 'b' + Integer.toBinaryString(value);
		}
	}

}
