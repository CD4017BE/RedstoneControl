package cd4017be.rs_ctr.circuit;

import java.util.UUID;
import cd4017be.rscpl.util.StateBuffer;

/**
 * Holds the internal state and other property data for a circuit whose implementation class hasn't been loaded yet.
 * @author CD4017BE
 */
public class UnloadedCircuit extends Circuit {

	private StateBuffer state = new StateBuffer();

	@Override
	public boolean tick() {return false;}

	@Override
	public void setState(StateBuffer state) {
		this.state = state;
	}

	@Override
	public StateBuffer getState() {
		return state;
	}

	@Override
	public Circuit load() {
		String name = name(ID);
		CircuitLoader.INSTANCE.register(name, null);
		Circuit c = CircuitLoader.INSTANCE.newCircuit(name);
		if (c == null) return this;
		c.deserializeNBT(serializeNBT());
		return c;
	}

	public static String name(UUID uid) {
		return "C_" + uid.toString().replace('-', '_');
	}

}
