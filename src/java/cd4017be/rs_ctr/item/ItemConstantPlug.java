package cd4017be.rs_ctr.item;

import java.util.function.Supplier;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.IConnector.IConnectorItem;
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
	protected IConnector create(ItemStack stack, EntityPlayer player) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) return null;
		Constant c = new Constant();
		c.deserializeNBT(nbt);
		return c;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		String s = super.getItemStackDisplayName(item);
		if (item.hasTagCompound()) s += " \u00a79" + item.getTagCompound().getInteger("val");
		return s;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking()) stack.setTagCompound(null);
		else openHeldItemGui(player, hand, 0, 0, 0);
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
			try {gui.sendPkt(Integer.parseInt(t));
			} catch (NumberFormatException e) {}
		}));
		gui.compGroup = frame;
		return gui;
	}

	static class StateInteractionHandler extends ItemInteractionHandler implements Supplier<String> {

		int value;

		public StateInteractionHandler(int slot) {
			super(Objects.constant, slot);
		}

		@Override
		protected void initSync(Builder sb) {
			sb.addFix(4);
		}

		@Override
		public void writeState(StateSyncServer state, AdvancedContainer cont) {
			state.buffer.writeInt(getNBT(cont.player).getInteger("val"));
			state.endFixed();
		}

		@Override
		public void readState(StateSyncClient state, AdvancedContainer cont) {
			value = state.get(value);
		}

		@Override
		public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
			getNBT(sender).setInteger("val", pkt.readInt());
		}

		@Override
		public String get() {
			return Integer.toString(value);
		}

	}

}
