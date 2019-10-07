package cd4017be.rs_ctr.port;

import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.IConnector;

/**
 * 
 * @author CD4017BE
 */
public enum WireType {
	SIGNAL(SignalHandler.class, "wire", "wireless", 0xff0000ff, "_plug.main(0)"),
	ENERGY(EnergyHandler.class, "wireE", "wirelessE", 0xff00ffff, "_plug.main(7)"),
	BLOCK(BlockHandler.class, "wireB", "wirelessB", 0xffffff00, "_plug.main(5)");

	public final String wiredId, wirelessId, model;
	public final Class<?> clazz;
	public final int color;

	private WireType(Class<?> clazz, String wiredId, String wirelessId, int color, String model) {
		this.clazz = clazz;
		this.wiredId = wiredId;
		this.wirelessId = wirelessId;
		this.color = color;
		this.model = model;
	}

	public static void registerAll() {
		for (WireType t : values()) {
			IConnector.REGISTRY.put(t.wiredId, ()-> new WireConnection(t));
			IConnector.REGISTRY.put(t.wirelessId, ()-> new WirelessConnection(t));
		}
	}
}