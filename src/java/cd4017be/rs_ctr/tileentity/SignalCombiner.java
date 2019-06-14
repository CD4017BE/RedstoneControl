package cd4017be.rs_ctr.tileentity;

import java.util.function.IntConsumer;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.rs_ctr.api.DelayedSignal;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;


/**
 * @author CD4017BE
 *
 */
public abstract class SignalCombiner extends WallMountGate implements IUpdatable {

	protected IntConsumer output = SignalReceiver.NOP;
	protected final int[] inputs = new int[4];
	protected byte tick;
	protected DelayedSignal delayed;

	{
		ports = new MountedSignalPort[] {
			new MountedSignalPort(this, 0, false).setName("port.rs_ctr.i"),
			new MountedSignalPort(this, 1, false).setName("port.rs_ctr.i"),
			new MountedSignalPort(this, 2, false).setName("port.rs_ctr.i"),
			new MountedSignalPort(this, 3, false).setName("port.rs_ctr.i"),
			new MountedSignalPort(this, 4, true).setName("port.rs_ctr.o")
		};
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
		return (val)-> setInput(pin, val);
	}

	@Override
	public void setPortCallback(int pin, IntConsumer callback) {
		if (callback == null) {
			output = SignalReceiver.NOP;
			dirty = 1;
		} else {
			if (output == SignalReceiver.NOP) dirty = 0;
			scheduleUpdate();
			output = callback;
		}
	}

	@Override
	public void process() {
		for (tick = 0; delayed != null; delayed = delayed.next, scheduleUpdate())
			inputs[delayed.id] = delayed.value;
	}

	protected void setInput(int pin, int val) {
		if (val != inputs[pin]) {
			if (tick == 0) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			} else if (tick != TickRegistry.TICK) {
				delayed = new DelayedSignal(pin, val, delayed);
				return;
			}
			inputs[pin] = val;
		}
	}

	protected void scheduleUpdate() {
		if (tick != 0) return;
		tick = TickRegistry.TICK;
		TickRegistry.schedule(this);
	}

	protected void refreshInput(int pin) {
		MountedSignalPort port = ports[pin];
		if (port.getConnector() != null) {
			port.onUnload();
			port.onLoad();
		} else getPortCallback(pin).accept(0);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) nbt.setIntArray("states", inputs);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			int[] arr = nbt.getIntArray("states");
			System.arraycopy(arr, 0, inputs, 0, Math.min(arr.length, inputs.length));
			tick = 0;
		}
	}

	protected void orient() {
		for (int i = 0; i < 4; i++)
			ports[i].setLocation(0.25F, 0.125F + i * 0.25F, 0.125F, EnumFacing.WEST, o);
		ports[4].setLocation(0.75F, 0.5F, 0.125F, EnumFacing.EAST, o);
	}

}
