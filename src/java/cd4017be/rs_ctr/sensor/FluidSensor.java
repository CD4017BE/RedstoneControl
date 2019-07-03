package cd4017be.rs_ctr.sensor;

import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.api.com.BlockReference;
import cd4017be.rs_ctr.api.sensor.IBlockSensor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;


/**
 * Reads the total amount of mB fluid stored in block's tanks.
 * @author CD4017BE
 */
public class FluidSensor implements IBlockSensor {

	public static final ResourceLocation MODEL = new ResourceLocation(Main.ID, "block/_sensor.fluid()");

	@Override
	public int readValue(BlockReference block) {
		IFluidHandler fh = block.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		if (fh == null) return 0;
		int val = 0;
		for (IFluidTankProperties prop : fh.getTankProperties()) {
			FluidStack stack = prop.getContents();
			if (stack != null) val += stack.amount;
		}
		return val;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.fluid");
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

}
