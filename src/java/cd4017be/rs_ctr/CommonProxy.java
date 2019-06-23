package cd4017be.rs_ctr;

import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.signal.BlockProbe;
import cd4017be.rs_ctr.signal.Clock;
import cd4017be.rs_ctr.signal.Constant;
import cd4017be.rs_ctr.signal.StatusLamp;
import cd4017be.rs_ctr.signal.WireType;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy {

	public void init() {
		WireType.registerAll();
		IConnector.REGISTRY.put(Constant.ID, Constant::new);
		IConnector.REGISTRY.put(StatusLamp.ID, StatusLamp::new);
		IConnector.REGISTRY.put(BlockProbe.ID, BlockProbe::new);
		IConnector.REGISTRY.put(Clock.ID, Clock::new);
	}

}
