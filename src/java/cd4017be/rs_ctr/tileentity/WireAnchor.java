package cd4017be.rs_ctr.tileentity;

import java.util.function.IntConsumer;

import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;


/**
 * @author CD4017BE
 *
 */
public class WireAnchor extends Gate {

	{ports = new MountedSignalPort[0];}

	@Override
	public IntConsumer getPortCallback(int pin) {
		return null;
	}

	@Override
	public void setPortCallback(int pin, IntConsumer callback) {
	}

	@Override
	public void onPortModified(SignalPort port, int event) {
		if (event == E_HOOK_REM && hooks.isEmpty() && !unloaded)
			world.setBlockToAir(pos);
		else super.onPortModified(port, event);
	}

}
