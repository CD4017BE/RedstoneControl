package cd4017be.rs_ctr;

import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.signal.BlockProbe;
import cd4017be.rs_ctr.signal.Constant;
import cd4017be.rs_ctr.signal.StatusLamp;
import cd4017be.rs_ctr.signal.WireConnection;
import cd4017be.rs_ctr.signal.WirelessConnection;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy {

	public void init() {
		IConnector.REGISTRY.put(WireConnection.ID, WireConnection.class);
		IConnector.REGISTRY.put(WirelessConnection.ID, WirelessConnection.class);
		IConnector.REGISTRY.put(Constant.ID, Constant.class);
		IConnector.REGISTRY.put(StatusLamp.ID, StatusLamp.class);
		IConnector.REGISTRY.put(BlockProbe.ID, BlockProbe.class);
	}

}
