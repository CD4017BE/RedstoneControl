package cd4017be.rs_ctr.tileentity;

import cd4017be.rs_ctr.api.com.BlockReference;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;


/**
 * @author CD4017BE
 *
 */
public class ItemReader extends Sensor {

	@Override
	protected int readValue(BlockReference ref) {
		IItemHandler ih = ref.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		if (ih == null) return 0;
		int val = 0;
		for (int i = ih.getSlots() - 1; i >= 0; i--)
			val += ih.getStackInSlot(i).getCount();
		return val;
	}

}
