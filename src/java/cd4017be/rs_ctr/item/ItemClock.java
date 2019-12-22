package cd4017be.rs_ctr.item;

import static cd4017be.lib.network.GuiNetworkHandler.openHeldItemGui;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.IConnector.IConnectorItem;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.ItemInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Spinner;
import cd4017be.lib.network.IGuiHandlerItem;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer.Builder;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.port.Clock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class ItemClock extends ItemPanelModule implements IConnectorItem, IGuiHandlerItem {

	/**
	 * @param id
	 */
	public ItemClock(String id) {
		super(id, "clock");
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		if (port.type != SignalHandler.class) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		} else if (port.isMaster) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.clock"));
			return;
		}
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) return;
		Clock c = new Clock();
		c.deserializeNBT(nbt);
		port.setConnector(c, player);
		if (!player.isCreative()) stack.shrink(1);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking()) stack.setTagCompound(null);
		else {
			if (!stack.hasTagCompound()) {
				NBTTagCompound nbt = new NBTTagCompound();
				nbt.setInteger("int", 20);
				stack.setTagCompound(nbt);
			}
			openHeldItemGui(player, hand, 0, 0, 0);
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
		GuiFrame frame = new GuiFrame(gui, 80, 58, 2)
				.title("gui.rs_ctr.clock.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 0, 31);
		new Spinner(frame, 36, 18, 37, 15, false, "\\%+.2fs", ()-> (double)state.phase / 20D, (v)-> gui.sendPkt((byte)1, (int)Math.round(v * 20D)), -60, 60, 1.0, 0.05).tooltip("gui.rs_ctr.phase");
		new Spinner(frame, 36, 18, 37, 33, false, "\\%.2fs", ()-> (double)state.interval / 20D, (v)-> gui.sendPkt((byte)0, (int)Math.round(v * 20D)), 0.05, 60, 1.0, 0.05).tooltip("gui.rs_ctr.interval");
		gui.compGroup = frame;
		return gui;
	}

	static class StateInteractionHandler extends ItemInteractionHandler {

		int phase, interval;

		public StateInteractionHandler(int slot) {
			super(Objects.clock, slot);
		}

		@Override
		protected void initSync(Builder sb) {
			sb.addMulFix(4, 2);
		}

		@Override
		public void writeState(StateSyncServer state, AdvancedContainer cont) {
			NBTTagCompound nbt = getNBT(cont.player);
			state.writeInt(nbt.getInteger("int")).writeInt(nbt.getInteger("pha")).endFixed();
		}

		@Override
		public void readState(StateSyncClient state, AdvancedContainer cont) {
			interval = state.get(interval);
			phase = state.get(phase);
		}

		@Override
		public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
			getNBT(sender).setInteger(pkt.readBoolean() ? "pha" : "int", pkt.readInt());
		}

	}

}
