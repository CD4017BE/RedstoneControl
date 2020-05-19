package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;
import cd4017be.api.rs_ctr.com.SignalHandler;

/** @author CD4017BE */
public class BlockReorienter extends WallMountGate implements BlockHandler, SignalHandler, IUpdatable {

	BlockReference in;
	BlockHandler out;
	int side;
	byte tick;
	boolean delayed;
	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setName("port.rs_ctr.bi").setLocation(0.25, 0.125, 0.125, EnumFacing.WEST),
			new MountedPort(this, 1, SignalHandler.class, false).setName("port.rs_ctr.side").setLocation(0.25, 0.875, 0.125, EnumFacing.WEST),
			new MountedPort(this, 2, BlockHandler.class, true).setName("port.rs_ctr.bo").setLocation(0.75, 0.5, 0.125, EnumFacing.EAST)
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		out = callback instanceof BlockHandler ? (BlockHandler)callback : null;
		if (out != null)
			out.updateBlock(out());
	}

	private BlockReference out() {
		if (side < 0 || in == null) return in;
		return new BlockReference(in.dim, in.pos, EnumFacing.VALUES[side], in.lifespan);
	}

	@Override
	public Object getState(int pin) {
		switch(pin) {
		case 0: return in;
		case 1: return side;
		case 2: return out();
		default: return null;
		}
	}
	
	@Override
	protected void resetPin(int pin) {
		if (pin == 0) updateBlock(null);
		else updateSignal(0);
	}

	@Override
	public void updateBlock(BlockReference ref) {
		if(BlockReference.equalDelayed(ref, in, 1)) return;
		scheduleUpdate();
		in = BlockReference.delayed(ref, 1);
	}

	@Override
	public void updateSignal(int value) {
		if(value < 0 || value >= 6) value = -1;
		if(value == side) return;
		scheduleUpdate();
		side = value;
	}

	private void scheduleUpdate() {
		if(tick == 0) {
			tick = TickRegistry.TICK;
			TickRegistry.schedule(this);
		} else if(tick != TickRegistry.TICK) {
			tick = TickRegistry.TICK;
			if(out != null)
				out.updateBlock(out());
			delayed = true;
		}
	}

	@Override
	public void process() {
		if(delayed) {
			delayed = false;
			TickRegistry.schedule(this);
			return;
		}
		tick = 0;
		if(out != null)
			out.updateBlock(out());
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode == SAVE) {
			if (in != null) nbt.setTag("block", in.serializeNBT());
			nbt.setByte("side", (byte)side);
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			in = nbt.hasKey("block", NBT.TAG_COMPOUND) ? new BlockReference(nbt.getCompoundTag("block")) : null;
			side = nbt.getByte("side");
			if (side >= 6) side = -1;
		}
	}

}
