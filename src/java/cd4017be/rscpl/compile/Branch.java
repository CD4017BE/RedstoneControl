package cd4017be.rscpl.compile;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.Pin;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

/**
 * Represents a combined chain of operations that don't share common inputs
 * @author CD4017BE
 */
public class Branch implements Operator, Comparable<Branch> {

	public static final byte IS_IN = 1, IS_OUT = 2;

	public final Operator result;
	public final LoadOp[] inputs;
	public final byte hasIO;
	int ordinal = -1;
	int localIdx;
	int uses;

	public static Branch from(Operator result) {
		ArrayList<LoadOp> list = new ArrayList<>();
		byte io = 0;
		if (addRecursive(list, result, false)) io = IS_IN;
		else if (result.isOutPin()) io = IS_OUT;
		return new Branch(result, list.toArray(new LoadOp[list.size()]), io);
	}

	private static boolean addRecursive(ArrayList<LoadOp> list, Operator op, boolean cond) {
		boolean isOut = op.isOutPin();
		boolean hasIn = op.isInPin();
		for (int i = 0, n = op.inputCount(); i < n; i++) {
			Operator o = op.getInput(i);
			if (o == null) continue;
			if (o.multiUse() || cond && o.hasSideEffects())
				list.add(new LoadOp(op, i, o));
			else if (isOut) {
				int size = list.size();
				if (addRecursive(list, o, cond | op.isConditional(i))) {
					List<LoadOp> sl = list.subList(size, list.size());
					o = new Branch(o, sl.toArray(new LoadOp[sl.size()]), IS_IN);
					sl.clear();
					list.add(new LoadOp(op, i, o));
				}
			} else if (addRecursive(list, o, cond | op.isConditional(i))) hasIn = true;
		}
		return hasIn;
	}

	public Branch(Operator result, LoadOp[] inputs, byte hasIO) {
		this.result = result;
		this.inputs = inputs;
		this.hasIO = hasIO;
	}

	@Override
	public InsnList compile(Context context) {
		InsnList ins = result.compile(context);
		Type t = result.outType();
		localIdx = context.newLocal(t);
		uses = result.receivers().size();
		ins.add(new VarInsnNode(t.getOpcode(ISTORE), localIdx));
		return ins;
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
		if (hasIO == IS_OUT) n += 65536;
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

}
