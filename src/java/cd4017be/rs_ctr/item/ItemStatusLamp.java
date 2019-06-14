package cd4017be.rs_ctr.item;

import java.util.function.IntConsumer;

import cd4017be.lib.item.BaseItem;
import cd4017be.rs_ctr.api.signal.IConnector.IConnectorItem;
import cd4017be.rs_ctr.signal.StatusLamp;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;


/**
 * @author CD4017BE
 *
 */
public class ItemStatusLamp extends BaseItem implements IConnectorItem {

	/**
	 * @param id
	 */
	public ItemStatusLamp(String id) {
		super(id);
	}

	@Override
	public void doAttach(ItemStack stack, MountedSignalPort port, EntityPlayer player) {
		if (port.type != IntConsumer.class) {
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
