package cd4017be.rs_ctr.tileentity;

import java.util.function.IntConsumer;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;


/**
 * @author CD4017BE
 *
 */
public abstract class SignalCombiner extends Gate implements IUpdatable {

	protected IntConsumer output = SignalReceiver.NOP;
	protected final int[] inputs = new int[4];
	protected boolean dirty;
	protected Orientation o = Orientation.N;

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
		return (val)-> {
			if (val != inputs[pin]) {
				inputs[pin] = val;
				scheduleUpdate();
			}
		};
	}

	@Override
	public void setPortCallback(int pin, IntConsumer callback) {
		if (callback == null) {
			output = SignalReceiver.NOP;
			dirty = true;
		} else {
			if (output == SignalReceiver.NOP) dirty = false;
			scheduleUpdate();
			output = callback;
		}
	}

	protected void scheduleUpdate() {
		if (dirty) return;
		dirty = true;
		TickRegistry.instance.updates.add(this);
	}

	protected void refreshInput(int pin) {
		MountedSignalPort port = ports[pin];
		if (port.getConnector() != null) {
			port.onUnload();
			port.onLoad();
		} else getPortCallback(pin).accept(0);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		int[] arr = nbt.getIntArray("states");
		System.arraycopy(arr, 0, inputs, 0, Math.min(arr.length, inputs.length));
		o = Orientation.values()[nbt.getByte("o") & 0xf];
		orient();
		dirty = false;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setIntArray("states", inputs);
		nbt.setByte("o", (byte)o.ordinal());
		return super.writeToNBT(nbt);
	}

	@Override
	public void updateContainingBlockInfo() {
		super.updateContainingBlockInfo();
		o = getOrientation();
		orient();
	}

	protected void orient() {
		for (int i = 0; i < 4; i++)
			ports[i].setLocation(0.25F, 0.125F + i * 0.25F, 0.125F, EnumFacing.WEST, o);
		ports[4].setLocation(0.75F, 0.5F, 0.125F, EnumFacing.EAST, o);
	}

}
