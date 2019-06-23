package cd4017be.rs_ctr.signal;

import cd4017be.rs_ctr.api.com.SignalHandler;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.com.EnergyHandler;

/**
 * 
 * @author CD4017BE
 */
public enum WireType {
	SIGNAL(SignalHandler.class, "wire", "wireless"),
	ENERGY(EnergyHandler.class, "wireE", "wirelessE");

	public final String wiredId, wirelessId;
	public final Class<?> clazz;

	private WireType(Class<?> clazz, String wiredId, String wirelessId) {
		this.clazz = clazz;
		this.wiredId = wiredId;
		this.wirelessId = wirelessId;
	}

	public static void registerAll() {
		for (WireType t : values()) {
			IConnector.REGISTRY.put(t.wiredId, ()-> new WireConnection(t));
			IConnector.REGISTRY.put(t.wirelessId, ()-> new WirelessConnection(t));
		}
	}
}