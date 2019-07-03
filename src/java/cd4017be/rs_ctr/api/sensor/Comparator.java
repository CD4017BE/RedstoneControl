package cd4017be.rs_ctr.api.sensor;

import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.com.BlockReference;
import net.minecraft.util.ResourceLocation;


/**
 * Simply reads the vanilla comparator value.
 * @author CD4017BE
 */
public class Comparator implements IBlockSensor {

	@Override
	public int readValue(BlockReference block) {
		return block.getState().getComparatorInputOverride(block.world, block.pos);
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.none");
	}

	@Override
	public ResourceLocation getModel() {
		return null;
	}

}
