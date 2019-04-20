package cd4017be.rs_ctr;

import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.signal.Constant;
import cd4017be.rs_ctr.signal.WireConnection;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy {

	public void init() {
		IConnector.REGISTRY.put(WireConnection.ID, WireConnection.class);
		IConnector.REGISTRY.put(Constant.ID, Constant.class);
	}

}
