package cd4017be.rs_ctr.item;

import static cd4017be.lib.network.GuiNetworkHandler.openHeldItemGui;

import java.util.function.Supplier;

import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.wire.IWiredConnector;
import cd4017be.api.rs_ctr.wire.WireLine;
import cd4017be.api.rs_ctr.wire.IWiredConnector.IWiredConnectorItem;
import cd4017be.api.rs_ctr.wire.WireLine.WireLoopException;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.ItemInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.network.IGuiHandlerItem;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer.Builder;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
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
public class ItemWireTag extends BaseItem implements IWiredConnectorItem, IGuiHandlerItem {

	/**
	 * @param id
	 */
	public ItemWireTag(String id) {
		super(id);
		setMaxStackSize(1);
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		String s = super.getItemStackDisplayName(item);
		if (item.hasTagCompound()) s += " \u00a7e" + item.getTagCompound().getString("name");
		return s;
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		IConnector c = port.getConnector();
		if (!(c instanceof ITagableConnector)) return;
		ITagableConnector tc = (ITagableConnector)c;
		if (stack.hasTagCompound()) {
			String tag = stack.getTagCompound().getString("name");
			if (!tag.equals(tc.getTag()))
				if (tc instanceof IWiredConnector) try {
					new WireLine(port).forEach(p -> {
						IConnector con = p.getConnector();
						if (con instanceof ITagableConnector)
							((ITagableConnector)con).setTag(p, tag);
					});
				} catch (WireLoopException e) {
					return;
				} else tc.setTag(port, tag);
			stack.setTagCompound(null);
		} else if (tc.getTag() != null) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setString("name", tc.getTag());
			stack.setTagCompound(nbt);
		}
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand hand) {
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
		GuiFrame frame = new GuiFrame(gui, 144, 31, 1)
				.title("gui.rs_ctr.tag.name", 0.5F)
				.background(new ResourceLocation(Main.ID, "textures/gui/small.png"), 80, 0);
		frame.add(new TextField(frame, 128, 7, 8, 16, 24, state, gui::sendPkt));
		gui.compGroup = frame;
		return gui;
	}

	static class StateInteractionHandler extends ItemInteractionHandler implements Supplier<String> {

		public StateInteractionHandler(int slot) {
			super(Objects.tag, slot);
		}

		String name = "";

		@Override
		protected void initSync(Builder sb) {
			sb.addVar(1);
		}

		@Override
		public void writeState(StateSyncServer state, AdvancedContainer cont) {
			state.endFixed().putAll(getNBT(cont.player).getString("name"));
		}

		@Override
		public void readState(StateSyncClient state, AdvancedContainer cont) {
			name = state.get(name);
		}

		@Override
		public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
			getNBT(sender).setString("name", pkt.readString(24));
		}

		@Override
		public String get() {
			return name;
		}

	}

}
