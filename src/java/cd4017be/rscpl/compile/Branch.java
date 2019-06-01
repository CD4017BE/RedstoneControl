package cd4017be.rscpl.compile;

import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.Pin;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Represents a combined chain of operations that don't share common inputs
 * @author CD4017BE
 */
public class Branch implements Operator, Comparable<Branch> {

	public final Operator result;
	public final LoadOp[] inputs;
	int ordinal = -1;
	int localIdx;
	int uses;

	public static Branch from(Operator result) {
		ArrayList<LoadOp> list = new ArrayList<>();
		addRecursive(list, result, false);
		return new Branch(result, list.toArray(new LoadOp[list.size()]));
	}

	private static void addRecursive(ArrayList<LoadOp> list, Operator op, boolean cond) {
		for (int i = 0, n = op.inputCount(); i < n; i++) {
			Operator o = op.getInput(i);
			if (o == null) continue;
			if (o.multiUse() || cond && o.hasSideEffects())
				list.add(new LoadOp(op, i, o));
			else
				addRecursive(list, o, cond | op.isConditional(i));
		}
	}

	public Branch(Operator result, LoadOp[] inputs) {
		this.result = result;
		this.inputs = inputs;
	}

	@Override
	public void compile(MethodVisitor mv, Context context) {
		result.compile(mv, context);
		Type t = result.outType();
		uses = result.receivers().size();
		if (uses > 0) {
			localIdx = context.newLocal(t);
			mv.visitVarInsn(t.getOpcode(ISTORE), localIdx);
		}
	}

	@Override
	public int inputCount() {
		return inputs.length;
	}

	@Override
	public Operator getInput(int pin) {
		return inputs[pin];
	}

	@Override
	public void setInput(int pin, Operator op) {
		inputs[pin] = (LoadOp)op;
	}

	@Override
	public Set<Pin> receivers() {
		return result.receivers();
	}

	@Override
	public Type outType() {
		return result.outType();
	}

	private int ordinal() {
		if (ordinal >= 0) return ordinal;
		int n = 0;
		for (LoadOp in : inputs)
			n = Math.max(n, ((Branch)in.getInput(0)).ordinal() + 1);
		return ordinal = n;
	}

	@Override
	public int compareTo(Branch o) {
		return this.ordinal() - o.ordinal();
	}

	@Override
	public Gate<?> getGate() { return null; }

	@Override
	public int getPin() { return 0; }

	@Override
	public Operator getActual() {
		return result.getActual();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(Arrays.toString(inputs));
		sb.setCharAt(0, '(');
		sb.setCharAt(sb.length() - 1, ')');
		sb.append("->").append(result);
		if (ordinal >= 0) sb.append('@').append(localIdx);
		return sb.toString();
	}

}
