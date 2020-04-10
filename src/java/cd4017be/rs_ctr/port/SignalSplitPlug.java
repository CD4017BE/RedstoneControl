package cd4017be.rs_ctr.port;

import java.util.Arrays;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.rs_ctr.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** @author CD4017BE */
public class SignalSplitPlug extends SplitPlug implements SignalHandler {

	SignalHandler[] callbacks;
	int lastValue;

	public SignalSplitPlug(MountedPort port) {
		super(port);
	}

	@Override
	public int addLinks(int n) {
		n = super.addLinks(n);
		callbacks = callbacks == null ? new SignalHandler[n] : Arrays.copyOf(callbacks, wires.length);
		Arrays.fill(callbacks, callbacks.length - n, callbacks.length, SignalHandler.NOP);
		return n;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		callbacks = new SignalHandler[wires.length];
		Arrays.fill(callbacks, SignalHandler.NOP);
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if(callback instanceof SignalHandler)
			(callbacks[pin - 1] = (SignalHandler)callback).updateSignal(lastValue);
		else callbacks[pin - 1] = SignalHandler.NOP;
	}

	@Override
	protected WireType type() {
		return WireType.SIGNAL;
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(Objects.split_s, wires.length);
	}

	@Override
	public void updateSignal(int value) {
		if(value == lastValue) return;
		lastValue = value;
		for(SignalHandler h : callbacks)
			h.updateSignal(value);
	}

}
