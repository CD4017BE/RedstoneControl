package cd4017be.rs_ctr.sensor;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;


/**
 * Reads the total amount of items stored in a block's inventory.
 * @author CD4017BE
 */
public class ItemSensor implements IBlockSensor {

	public static final ResourceLocation MODEL = new ResourceLocation(Main.ID, "block/_sensor.item()");

	@Override
	public int readValue(BlockReference block) {
		IItemHandler ih = block.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		if (ih == null) return 0;
		int val = 0;
		for (int i = ih.getSlots() - 1; i >= 0; i--)
			val += ih.getStackInSlot(i).getCount();
		return val;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.item");
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

}
