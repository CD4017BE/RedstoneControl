package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.IConnector.IConnectorItem;
import cd4017be.rs_ctr.port.StatusLamp;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;


/**
 * @author CD4017BE
 *
 */
public class ItemStatusLamp extends ItemPanelModule implements IConnectorItem {

	/**
	 * @param id
	 */
	public ItemStatusLamp(String id) {
		super(id, "lamp");
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		if (port.type != SignalHandler.class) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		} else if (!port.isMaster) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.lamp"));
			return;
		}
		port.setConnector(new StatusLamp(), player);
		if (!player.isCreative()) stack.shrink(1);
	}

}
