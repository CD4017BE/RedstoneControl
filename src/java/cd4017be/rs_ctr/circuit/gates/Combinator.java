package cd4017be.rs_ctr.circuit.gates;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.Pin;

/**
 * A gate with a single output.
 * @author CD4017BE
 */
public class Combinator extends Gate<BasicType> implements Operator {

	protected final Set<Pin> receivers;

	/**
	 * @param type
	 * @param index
	 */
	public Combinator(BasicType type, int index) {
		super(type, index);
		receivers = outputCount() > 0 ? new HashSet<>() : Collections.emptySet();
	}

	@Override
	public InsnList compile(Context context) {
		return type.outputs[0].compile(context, inputs);
	}

	@Override
	public Set<Pin> receivers() {
		return receivers;
	}

	@Override
	public Type outType() {
		return type.outputs[0].result;
	}

	@Override
	public Combinator getGate() {
		return this;
	}

	@Override
	public int getPin() {
		return 0;
	}

	@Override
	protected boolean requiresInput(int pin) {
		return true;
	}

	@Override
	public int outputCount() {
		return 1;
	}

	@Override
	public Operator getOutput(int pin) {
		return this;
	}

	@Override
	public int getInputHeight(int pin) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getOutputHeight(int pin) {
		// TODO Auto-generated method stub
		return 0;
	}

}