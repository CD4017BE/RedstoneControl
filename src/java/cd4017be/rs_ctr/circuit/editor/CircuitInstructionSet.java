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

	public static final BasicType
		in = new BasicType(Input::new, 1, 3, 0, new Code(INT_TYPE, "%p$", ALOAD, IALOAD)),
		out = new BasicType(Output::new, 1, 3, 1, new Code(INT_TYPE, "%p$>*!E	*%p**	%t=:callbacks [I$**;java/util/function/IntConsumer:accept(I)V	*%m*	|E",
			ALOAD, IALOAD, DUP_X1, IF_ICMPEQ	, DUP, ALOAD, SWAP, IASTORE 	, ALOAD, GETFIELD, AALOAD, SWAP, INVOKEINTERFACE	, ICONST_1, ISTORE, ICONST_0	, POP)),
		i_cst = new BasicType(ConstNum::new, 5, 2, 0, new Code(INT_TYPE, null)),
		not = n(1, new Code(INT_TYPE, ">**", ICONST_M1, IXOR)),
		or = n(2, new Code(INT_TYPE, "0>1>*", IOR)),
		nor = n(2, new Code(INT_TYPE, "0>1>3*", IOR, ICONST_M1, IXOR)),
		and = n(2, new Code(INT_TYPE, "0>1>*", IAND)),
		nand = n(2, new Code(INT_TYPE, "0>1>3*", IAND, ICONST_M1, IXOR)),
		xor = n(2, new Code(INT_TYPE, "0>1>*", IXOR)),
		xnor = n(2, new Code(INT_TYPE, "0>1>3*", IXOR, ICONST_M1, IXOR)),
		not0 = n(1, new Code(INT_TYPE, ">!N*!X|N*|X", IFNE, ICONST_0, GOTO, ICONST_M1)),
		is0 = n(1, new Code(INT_TYPE, ">!E*!X|E*|X", IFEQ, ICONST_0, GOTO, ICONST_M1)),
		nsgn = n(1, new Code(INT_TYPE, ">!L*!X|L*|X", IFLT, ICONST_0, GOTO, ICONST_M1)),
		psgn = n(1, new Code(INT_TYPE, ">!G*!X|G*|X", IFGE, ICONST_0, GOTO, ICONST_M1)),
		eq = n(2, new Code(INT_TYPE, "0>1>!E*!X|E*|X", IF_ICMPEQ, ICONST_0, GOTO, ICONST_M1)),
		neq = n(2, new Code(INT_TYPE, "0>1>!N*!X|N*|X", IF_ICMPNE, ICONST_0, GOTO, ICONST_M1)),
		ls = n(2, new Code(INT_TYPE, "0>1>!L*!X|L*|X", IF_ICMPLT, ICONST_0, GOTO, ICONST_M1)),
		geq = n(2, new Code(INT_TYPE, "0>1>!G*!X|G*|X", IF_ICMPGE, ICONST_0, GOTO, ICONST_M1)),
		inc = n(1, new Code(INT_TYPE, ">**", ICONST_1, IADD)),
		dec = n(1, new Code(INT_TYPE, ">**", ICONST_1, ISUB)),
		neg = n(1, new Code(INT_TYPE, ">*", INEG)),
		abs = n(1, new Code(INT_TYPE, ">*!X*|X", DUP, IFGE, INEG)),
		add = n(2, new Code(INT_TYPE, "0>1>*", IADD)),
		sub = n(2, new Code(INT_TYPE, "0>1>*", ISUB)),
		mul = n(2, new Code(INT_TYPE, "0>1>*", IMUL)),
		div = n(2, new Code(INT_TYPE, "0>1>;java/lang/Math:floorDiv(II)I", INVOKESTATIC)),
		mod = n(2, new Code(INT_TYPE, "0>1>;java/lang/Math:floorMod(II)I", INVOKESTATIC)),
		bsl = n(2, new Code(INT_TYPE, "0>1>*", ISHL)),
		bsr = n(2, new Code(INT_TYPE, "0>1>*", ISHR)),
		usr = n(2, new Code(INT_TYPE, "0>1>*", IUSHR)),
		max = n(2, new Code(INT_TYPE, "0>1>*!X*|X*", DUP2, IF_ICMPGE, SWAP, POP)),
		min = n(2, new Code(INT_TYPE, "0>1>*!X*|X*", DUP2, IF_ICMPLE, SWAP, POP)),
		swt = n(3, new Code(INT_TYPE, "0>!A1>!X|A2>|X", IFNE, GOTO));

	static {
		INS_SET.add(0, in, out, i_cst);//TODO add all
	}

	private static BasicType n(int in, Code out) {
		return new BasicType(Combinator::new, 3, in, in, out);
	}

	public final int[] OP_COSTS = new int[256];

	public int getCost(GateType<?> t) {
		return OP_COSTS[t.id()];
	}
}
