package cd4017be.rs_ctr.port;

import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.IConnector;
import net.minecraft.item.Item;

/**
 * 
 * @author CD4017BE
 */
public enum WireType {
	SIGNAL(SignalHandler.class, "wire", "wireless", "split", 0xff0000ff),
	ENERGY(EnergyHandler.class, "wireE", "wirelessE", "splitE", 0xff00ffff),
	BLOCK(BlockHandler.class, "wireB", "wirelessB", "splitB", 0xffffff00);

	public final String wiredId, wirelessId, splitId;
	public final Class<?> clazz;
	public final int color;
	public Item wireItem, wirelessItem;

	private WireType(Class<?> clazz, String wiredId, String wirelessId, String splitId, int color) {
		this.clazz = clazz;
		this.wiredId = wiredId;
		this.wirelessId = wirelessId;
		this.splitId = splitId;
		this.color = color;
	}

	public static void registerAll() {
		for (WireType t : values()) {
			IConnector.REGISTRY.put(t.wiredId, ()-> new WireConnection(t));
			IConnector.REGISTRY.put(t.wirelessId, ()-> new WirelessConnection(t));
		}
	}

	public String wireModel() {
		return "_plug.wire(" + ordinal() + ")";
	}

	public String wirelessModel() {
		return "_plug.wireless(" + ordinal() + ")";
	}
}