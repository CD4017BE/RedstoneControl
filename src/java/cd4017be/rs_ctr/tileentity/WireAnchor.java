package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;


/**
 * @author CD4017BE
 *
 */
public class WireAnchor extends Gate {

	{ports = new MountedPort[0];}

	@Override
	public Object getPortCallback(int pin) {
		return null;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
	}

	@Override
	protected void resetPin(int pin) {
	}

	@Override
	public void onPortModified(Port port, int event) {
		if (event == E_HOOK_REM && hooks.isEmpty() && !unloaded)
			world.setBlockToAir(pos);
		else super.onPortModified(port, event);
	}

}
