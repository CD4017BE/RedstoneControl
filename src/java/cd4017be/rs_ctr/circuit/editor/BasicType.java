package cd4017be.rs_ctr.circuit.editor;

import org.objectweb.asm.Type;

import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.compile.Dep;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.compile.NodeCompiler;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.editor.Pin;


/**
 * @author CD4017BE
 *
 */
public class BasicType extends GateType {

	public final int inputs;
	public final NodeCompiler[] outputs;
	private final GateConstructor constr;

	/**
	 * @param constr
	 * @param name
	 * @param width
	 * @param height
	 * @param inputs
	 * @param outputs
	 */
	public BasicType(GateConstructor constr, String name, int width, int height, int inputs, NodeCompiler... outputs) {
		super(Main.ID + ":" + name, width, height);
		this.constr = constr;
		this.inputs = inputs;
		this.outputs = outputs;
	}

	@Override
	public Gate newGate(int index) {
		return constr.newGate(this, index, inputs, outputs.length);
	}

	@Override
	public int getInputHeight(int i) {
		return i == 1 && inputs == 2 ? 2 : i;
	}

	@Override
	public int getOutputHeight(int o) {
		return 1;
	}

	@Override
	public Type getOutType(int o) {
		return outputs[o].getOutType();
	}

	@Override
	public boolean isInputTypeValid(int i, Type type) {
		return Dep.canConvert(type, outputs[0].getInType(i));
	}

	@Override
	public Node createNode(Gate gate, int o) {
		if (gate instanceof ISpecialNodeProvider)
			return ((ISpecialNodeProvider)gate).createNode(o, outputs[o]);
		Node[] ins = new Node[gate.inputCount()];
		for (int i = 0; i < ins.length; i++) {
			Pin pin = gate.getInput(i);
			if (pin != null) ins[i] = pin.getNode();
		}
		return new Node(outputs[o], ins);
	}

	/**
	 * For gates that want to create the nodes on their own.
	 * @author cd4017be
	 */
	public interface ISpecialNodeProvider {

		/**
		 * @param o output index to create for
		 * @return created node
		 */
		Node createNode(int o, NodeCompiler code);

	}

}
