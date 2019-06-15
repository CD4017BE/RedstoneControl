package cd4017be.rs_ctr.tileentity;

import cd4017be.rs_ctr.api.com.SignalHandler;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;


/**
 * @author CD4017BE
 *
 */
public class SignalSplitter extends WallMountGate {

	protected final SignalHandler[] callbacks = new SignalHandler[4];
	protected int state;

	{
		ports = new MountedSignalPort[] {
			new MountedSignalPort(this, 0, SignalHandler.class, true).setName("port.rs_ctr.o"),
			new MountedSignalPort(this, 1, SignalHandler.class, true).setName("port.rs_ctr.o"),
			new MountedSignalPort(this, 2, SignalHandler.class, true).setName("port.rs_ctr.o"),
			new MountedSignalPort(this, 3, SignalHandler.class, true).setName("port.rs_ctr.o"),
			new MountedSignalPort(this, 4, SignalHandler.class, false).setName("port.rs_ctr.i")
		};
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		return (val)-> {
			if (val == state) return;
			state = val;
			for (SignalHandler c : callbacks)
				if (c != null)
					c.updateSignal(val);
		};
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		SignalHandler c = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		callbacks[pin] = c;
		if (c != null) c.updateSignal(state);
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).updateSignal(0);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) nbt.setInteger("state", state);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) state = nbt.getInteger("state");
		super.loadState(nbt, mode);
	}

	protected void orient() {
		for (int i = 0; i < 4; i++)
			ports[i].setLocation(0.75F, 0.125F + i * 0.25F, 0.125F, EnumFacing.EAST, o);
		ports[4].setLocation(0.25F, 0.5F, 0.125F, EnumFacing.WEST, o);
	}

}
