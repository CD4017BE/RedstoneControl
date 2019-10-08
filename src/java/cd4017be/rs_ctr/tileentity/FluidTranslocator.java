package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * @author CD4017BE
 *
 */
public class FluidTranslocator extends WallMountGate implements ITickableServerOnly {

	public static int BASE_COST = -100, TRANSFER_COST = -500;

	BlockReference ref0, ref1;
	EnergyHandler energy = EnergyHandler.NOP;
	SignalHandler out;
	int amIn, amOut, clk;
	boolean update;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setLocation(0.875, 0.625, 0.25, EnumFacing.NORTH).setName("port.rs_ctr.tank0"),
			new MountedPort(this, 1, BlockHandler.class, false).setLocation(0.125, 0.625, 0.25, EnumFacing.NORTH).setName("port.rs_ctr.tank1"),
			new MountedPort(this, 2, EnergyHandler.class, true).setLocation(0.375, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.energy_i"),
			new MountedPort(this, 3, SignalHandler.class, false).setLocation(0.625, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.clk"),
			new MountedPort(this, 4, SignalHandler.class, false).setLocation(0.875, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.am_i"),
			new MountedPort(this, 5, SignalHandler.class, true).setLocation(0.125, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.am_o"),
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		switch(pin) {
		case 0: return (BlockHandler)(ref)-> ref0 = ref;
		case 1: return (BlockHandler)(ref)-> ref1 = ref;
		case 3: return (SignalHandler)(val)-> {
			if (val == clk) return;
			clk = val;
			update = true;
		};
		case 4: return (SignalHandler)(val)-> amIn = val;
		default: return null;
		}
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		switch(pin) {
		case 2:
			energy = callback instanceof EnergyHandler ? (EnergyHandler)callback : EnergyHandler.NOP;
			break;
		case 5:
			if (callback instanceof SignalHandler) {
				out = (SignalHandler)callback;
				out.updateSignal(amOut);
			} else out = null;
			break;
		}
	}

	@Override
	protected void resetPin(int pin) {
		switch(pin) {
		case 0: ref0 = null; break;
		case 1: ref1 = null; break;
		case 3: clk = 0; break;
		case 4: amIn = 0; break;
		}
	}

	@Override
	public void update() {
		if (!update) return;
		update = false;
		int n = transfer(amIn);
		if (amOut != n) {
			amOut = n;
			if (out != null)
				out.updateSignal(n);
		}
	}

	private int transfer(int am) {
		if (am == 0 || ref0 == null || ref1 == null || !ref0.isLoaded() || !ref1.isLoaded()) return 0;
		int e = cost(am);
		if (energy.changeEnergy(e, true) != e) return 0;
		IFluidHandler inv0 = ref0.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		IFluidHandler inv1 = ref1.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		if (am > 0) am = transfer(am, inv0, inv1);
		else am = -transfer(-am, inv1, inv0);
		energy.changeEnergy(cost(am), false);
		return am;
	}

	private static int cost(int am) {
		if (am < 0) am = -am;
		return (int)Math.min((long)am * (long)TRANSFER_COST / 1000L + (long)BASE_COST, Integer.MAX_VALUE);
	}

	private int transfer(int am, IFluidHandler fromInv, IFluidHandler toInv) {
		if (fromInv == null || toInv == null) return 0;
		FluidStack stack = fromInv.drain(am, false);
		if (stack == null) am = 0;
		else if ((am = toInv.fill(stack, true)) > 0)
			fromInv.drain(new FluidStack(stack, am), true);
		return am;
	}

}
