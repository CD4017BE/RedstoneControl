package cd4017be.rs_ctr.api.circuitgraph;

/**
 * 
 * @author CD4017BE
 */
public class Pin {

	public final Operator op;
	public final int idx;

	public Pin(Operator op, int pin) {
		this.op = op;
		this.idx = pin;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + idx;
		result = prime * result + op.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Pin)) return false;
		Pin other = (Pin)obj;
		return idx == other.idx && op == other.op;
	}

}