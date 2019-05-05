package cd4017be.rs_ctr.circuit.editor;

import static org.objectweb.asm.Type.INT_TYPE;

import cd4017be.rs_ctr.circuit.gates.Combinator;
import cd4017be.rs_ctr.circuit.gates.ConstNum;
import cd4017be.rs_ctr.circuit.gates.Input;
import cd4017be.rs_ctr.circuit.gates.Output;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.editor.InstructionSet;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author CD4017BE
 *
 */
public class CircuitInstructionSet extends InstructionSet {

	public static final CircuitInstructionSet INS_SET = new CircuitInstructionSet();

	public final int[] OP_COSTS = new int[256];

	public int getCost(GateType<?> t) {
		return OP_COSTS[t.id()];
	}
}
