package cd4017be.rs_ctr.tileentity;

import cd4017be.rs_ctr.api.com.BlockReference;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;


/**
 * @author CD4017BE
 *
 */
public class FluidReader extends Sensor {

	@Override
	protected int readValue(BlockReference ref) {
		IFluidHandler fh = ref.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		if (fh == null) return 0;
		int val = 0;
		for (IFluidTankProperties prop : fh.getTankProperties()) {
			FluidStack stack = prop.getContents();
			if (stack != null) val += stack.amount;
		}
		return val;
	}

}
