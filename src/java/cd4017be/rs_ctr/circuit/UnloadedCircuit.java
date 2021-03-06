package cd4017be.rs_ctr.circuit;

import java.util.UUID;
import cd4017be.rscpl.util.StateBuffer;

/**
 * Holds the internal state and other property data for a circuit whose implementation class hasn't been loaded yet.
 * @author CD4017BE
 */
public class UnloadedCircuit extends Circuit {

	private StateBuffer state = new StateBuffer();

	{
		inputs = new int[0];
		outputs = new int[0];
	}

	@Override
	public int tick() {return 0;}

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
		if (ID == null) return this;
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
