package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.IConnector.IConnectorItem;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.rs_ctr.port.PulseGen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** @author CD4017BE */
public class ItemPulseGen extends ItemPlug implements IConnectorItem {

	public ItemPulseGen(String id) {
		super(id, SignalHandler.class, true);
	}

	@Override
	protected IConnector create(ItemStack stack, EntityPlayer player) {
		return new PulseGen();
	}

}
