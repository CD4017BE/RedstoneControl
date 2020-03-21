package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.IStateInfo;
import cd4017be.api.rs_ctr.port.MountedPort;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;


/**
 * @author CD4017BE
 *
 */
public class SignalSplitter extends WallMountGate implements IStateInfo {

	protected final SignalHandler[] callbacks = new SignalHandler[4];
	protected int state;

	{
		ports = new MountedPort[5];
		for (int i = 0; i < 4; i++)
			ports[i] = new MountedPort(this, i, SignalHandler.class, true).setLocation(0.75F, 0.125F + i * 0.25F, 0.125F, EnumFacing.EAST).setName("port.rs_ctr.o");
		ports[4] = new MountedPort(this, 4, SignalHandler.class, false).setLocation(0.25F, 0.5F, 0.125F, EnumFacing.WEST).setName("port.rs_ctr.i");
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

	@Override
	public Object getState(int id) {
		return state;
	}

}
