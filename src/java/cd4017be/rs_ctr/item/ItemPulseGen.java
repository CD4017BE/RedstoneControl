package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.Connector.IConnectorItem;
import cd4017be.api.rs_ctr.port.MountedPort;
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
	protected Connector create(MountedPort port, ItemStack stack, EntityPlayer player) {
		return new PulseGen(port);
	}

}
