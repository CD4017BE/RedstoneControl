package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/** @author CD4017BE */
public class SignalDelayer extends WallMountGate {

	private final Delay[] channels = new Delay[4];

	{
		ports = new MountedPort[8];
		for(int i = 0; i < 4; i++) {
			ports[i] = new MountedPort(this, i, SignalHandler.class, false)
			.setLocation(0.25F, 0.125F + i * 0.25F, 0.125F, EnumFacing.WEST)
			.setName("port.rs_ctr.i");
			ports[i + 4] = new MountedPort(this, i + 4, SignalHandler.class, true)
			.setLocation(0.75F, 0.125F + i * 0.25F, 0.125F, EnumFacing.EAST)
			.setName("port.rs_ctr.o");
			channels[i] = new Delay();
		}
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		return channels[pin];
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		Delay c = channels[pin - 4];
		if(callback instanceof SignalHandler)
			(c.out = (SignalHandler)callback).updateSignal(c.state);
		else c.out = SignalHandler.NOP;
	}

	@Override
	protected void resetPin(int pin) {
		channels[pin].updateSignal(0);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if(mode == SAVE) {
			int[] states = new int[4];
			for(int i = 0; i < 4; i++)
				states[i] = channels[i].state;
			nbt.setIntArray("states", states);
		}
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if(mode == SAVE) {
			int[] states = nbt.getIntArray("states");
			for(int i = 0; i < 4 && i < states.length; i++)
				channels[i].state = states[i];
		}
	}


	static class Delay implements SignalHandler, IUpdatable {

		SignalHandler out = SignalHandler.NOP;
		int state;
		byte tick;

		@Override
		public void updateSignal(int value) {
			if(value == state) return;
			if(tick == 0) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			} else if(tick != TickRegistry.TICK) {
				tick = TickRegistry.TICK;
				out.updateSignal(state);
			}
			state = value;
		}

		@Override
		public void process() {
			if(tick == TickRegistry.TICK)
				TickRegistry.schedule(this);
			else {
				tick = 0;
				out.updateSignal(state);
			}
		}

	}

}
