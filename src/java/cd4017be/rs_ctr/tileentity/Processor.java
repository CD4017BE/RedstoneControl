package cd4017be.rs_ctr.tileentity;

import java.util.function.IntConsumer;

import cd4017be.rs_ctr.api.signal.MountedSignalPort;


/**
 * @author CD4017BE
 *
 */
public class Processor extends Gate {

	{ports = new MountedSignalPort[0];}

	@Override
	public IntConsumer getPortCallback(int pin) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPortCallback(int pin, IntConsumer callback) {
		// TODO Auto-generated method stub

	}

}
