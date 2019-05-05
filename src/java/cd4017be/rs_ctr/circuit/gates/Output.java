package cd4017be.rs_ctr.circuit.gates;

import cd4017be.rs_ctr.circuit.editor.BasicType;

/**
 * @author CD4017BE
 *
 */
public class Output extends Combinator {

	/**
	 * @param type
	 * @param index
	 */
	public Output(BasicType type, int index) {
		super(type, index);
	}

	@Override
	public boolean isOutPin() {
		return true;
	}

	@Override
	public boolean hasSideEffects() {
		return true;
	}

	@Override
	public int outputCount() {
		return 0; //invisible output pin
	}

}
