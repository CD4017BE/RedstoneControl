package cd4017be.rs_ctr.sensor;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;


/**
 * Reads the amount of Forge Energy units stored in a block.
 * @author CD4017BE
 */
public class ForgeEnergySensor implements IBlockSensor {

	public static final ResourceLocation MODEL = new ResourceLocation(Main.ID, "block/_sensor.fe()");

	@Override
	public int readValue(BlockReference block) {
		IEnergyStorage es = block.getCapability(CapabilityEnergy.ENERGY);
		return es != null ? es.getEnergyStored() : 0;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.fe");
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

}
