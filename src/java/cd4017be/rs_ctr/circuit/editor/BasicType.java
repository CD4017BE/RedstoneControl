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
	private final GateConstructor<BasicType> constr;

	/**
	 * @param constr
	 * @param width
	 * @param height
	 * @param inputs
	 * @param output
	 */
	public BasicType(GateConstructor<BasicType> constr, String name, int width, int height, int inputs, Code... outputs) {
		super(Main.ID + ":" + name, width, height, inputs);
		this.outputs = outputs;
		this.constr = constr;
	}

	@Override
	public Gate<BasicType> newGate(int index) {
		return constr.newGate(this, index);
	}

}
