package cd4017be.rs_ctr.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.Utils;

/**
 * 
 * @author cd4017be
 */
public class EnergyValve extends WallMountGate implements IEnergyStorage, SignalHandler {

	public static final IEnergyStorage NULL_RECEIVE = new IEnergyStorage() {
		@Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
		@Override public int getMaxEnergyStored() { return 0; }
		@Override public int getEnergyStored() { return 0; }
		@Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
		@Override public boolean canReceive() { return false; }
		@Override public boolean canExtract() { return true; }
	};

	SignalHandler flowOut;
	IEnergyStorage out;
	int amIn, flow, limit, amOut, clk;
	boolean update = true;
	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, SignalHandler.class, false).setName("port.rs_ctr.clk").setLocation(0.25, 0.625, 0.5, EnumFacing.WEST),
			new MountedPort(this, 1, SignalHandler.class, false).setName("port.rs_ctr.rf_i").setLocation(0.25, 0.375, 0.375, EnumFacing.WEST),
			new MountedPort(this, 2, SignalHandler.class, true).setName("port.rs_ctr.rf_o").setLocation(0.25, 0.375, 0.625, EnumFacing.WEST)
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		return pin == 0 ? this : (SignalHandler)(v) -> amIn = v;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		flowOut = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (flowOut != null) flowOut.updateSignal(amOut);
	}

	@Override
	protected void resetPin(int pin) {
		if (pin == 1) amIn = 0;
	}

	@Override
	public void updateSignal(int value) {//clock
		if (value == clk) return;
		clk = value;
		update = true;
		if (flow != amOut)
			flowOut.updateSignal(amOut = flow);
		flow = 0;
		markDirty(SAVE);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode == SAVE) {
			nbt.setInteger("amIn", amIn);
			nbt.setInteger("flow", flow);
			nbt.setInteger("amOut", amOut);
			nbt.setInteger("clk", clk);
			nbt.setBoolean("update", update);
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			limit = amIn = nbt.getInteger("amIn");
			flow = nbt.getInteger("flow");
			amOut = nbt.getInteger("amOut");
			clk = nbt.getInteger("clk");
			update = nbt.getBoolean("update");
		}
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		EnumFacing front = getOrientation().front;
		return cap == CapabilityEnergy.ENERGY && (facing == front || facing == front.getOpposite());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap == CapabilityEnergy.ENERGY) {
			EnumFacing front = getOrientation().front;
			if (facing == front) {
				out = Utils.neighborCapability(this, front.getOpposite(), CapabilityEnergy.ENERGY);
				return (T) this;
			} else if (facing == front.getOpposite()) return (T) NULL_RECEIVE;
		}
		return null;
	}

	@Override
	public int receiveEnergy(int am, boolean sim) {
		if (update) {
			update = false;
			limit = amIn;
			markDirty(SAVE);
		}
		if (am + flow > limit) am = limit - flow;
		if (am > 0 && out != null) {
			am = out.receiveEnergy(am, sim);
			if (!sim) flow += am;
			return am;
		}
		return 0;
	}

	@Override
	public int extractEnergy(int am, boolean sim) {
		return 0;
	}

	@Override
	public int getEnergyStored() {
		return flow;
	}

	@Override
	public int getMaxEnergyStored() {
		return limit;
	}

	@Override
	public boolean canExtract() {
		return false;
	}

	@Override
	public boolean canReceive() {
		return true;
	}

}
