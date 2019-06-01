package cd4017be.rs_ctr.circuit.editor;

import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;


/**
 * @author CD4017BE
 *
 */
public class BasicType extends GateType<BasicType> {

	public final Code[] outputs;
	public final int invisibleInputs;
	private final GateConstructor<BasicType> constr;

	/**
	 * @param constr
	 * @param width
	 * @param height
	 * @param inputs
	 * @param output
	 */
	public BasicType(GateConstructor<BasicType> constr, String name, int width, int height, int inputs, Code... outputs) {
		this(constr, name, width, height, inputs, 0, outputs);
	}

	/**
	 * @param constr
	 * @param width
	 * @param height
	 * @param inputs
	 * @param invIn
	 * @param output
	 */
	public BasicType(GateConstructor<BasicType> constr, String name, int width, int height, int inputs, int invIn, Code... outputs) {
		super(Main.ID + ":" + name, width, height, inputs);
		this.outputs = outputs;
		this.constr = constr;
		this.invisibleInputs = invIn;
	}

	@Override
	public Gate<BasicType> newGate(int index) {
		return constr.newGate(this, index);
	}

}
