package cd4017be.api.rs_ctr.port;

import cd4017be.api.rs_ctr.port.Port;

/**
 * @author CD4017BE
 *
 */
public interface IStateInfo {

	Port[] availablePorts();

	Object getState(int pin);

}
