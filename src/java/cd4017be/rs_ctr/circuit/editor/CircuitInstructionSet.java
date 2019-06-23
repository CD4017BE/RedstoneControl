package cd4017be.rs_ctr.circuit.editor;

import static org.objectweb.asm.Type.INT_TYPE;

import org.objectweb.asm.Type;

import cd4017be.lib.script.obj.Error;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Vector;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.circuit.gates.Combinator;
import cd4017be.rs_ctr.circuit.gates.ConstNum;
import cd4017be.rs_ctr.circuit.gates.Input;
import cd4017be.rs_ctr.circuit.gates.Output;
import cd4017be.rs_ctr.circuit.gates.ReadVar;
import cd4017be.rs_ctr.circuit.gates.WriteVar;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.editor.InstructionSet;
import cd4017be.rscpl.gui.Category;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author CD4017BE
 *
 */
public class CircuitInstructionSet extends InstructionSet implements IOperand {

	public static final CircuitInstructionSet INS_SET = new CircuitInstructionSet();
	public static Category[] TABS;

	public static final BasicType
		in = new BasicType(Input::new, "in", 1, 2, 1, 1, new Code(INT_TYPE, ">$*", IALOAD)),
		out = new BasicType(Output::new, "out", 1, 2, 4, 3, new Code(INT_TYPE, "2>$*0>!E 3>$1>* %m1$*%m |E", IALOAD, IF_ICMPEQ, IASTORE, ILOAD, IOR, ISTORE)),
		read = new BasicType(ReadVar::new, "read", 6, 2, 0, new Code(INT_TYPE, null)),
		write = new BasicType(WriteVar::new, "write", 6, 2, 2, 1, new Code(INT_TYPE, "0>*1>!E*%t*=: I %m**%m |E", DUP, IF_ICMPEQ, DUP, ALOAD, SWAP, PUTFIELD, ILOAD, ICONST_1, IOR, ISTORE)),
		i_cst = new BasicType(ConstNum::new, "i_cst", 6, 2, 0, new Code(INT_TYPE, "$")),
		not = n("not", 1, new Code(INT_TYPE, ">**", ICONST_M1, IXOR)),
		or = n("or", 2, new Code(INT_TYPE, "0>1>*", IOR)),
		nor = n("nor", 2, new Code(INT_TYPE, "0>1>3*", IOR, ICONST_M1, IXOR)),
		and = n("and", 2, new Code(INT_TYPE, "0>1>*", IAND)),
		nand = n("nand", 2, new Code(INT_TYPE, "0>1>3*", IAND, ICONST_M1, IXOR)),
		xor = n("xor", 2, new Code(INT_TYPE, "0>1>*", IXOR)),
		xnor = n("xnor", 2, new Code(INT_TYPE, "0>1>3*", IXOR, ICONST_M1, IXOR)),
		not0 = n("not0", 1, new Code(INT_TYPE, ">!N*!X|N*|X", IFNE, ICONST_0, GOTO, ICONST_M1)),
		is0 = n("is0", 1, new Code(INT_TYPE, ">!E*!X|E*|X", IFEQ, ICONST_0, GOTO, ICONST_M1)),
		nsgn = n("nsgn", 1, new Code(INT_TYPE, ">!L*!X|L*|X", IFLT, ICONST_0, GOTO, ICONST_M1)),
		psgn = n("psgn", 1, new Code(INT_TYPE, ">!G*!X|G*|X", IFGE, ICONST_0, GOTO, ICONST_M1)),
		eq = n("eq", 2, new Code(INT_TYPE, "0>1>!E*!X|E*|X", IF_ICMPEQ, ICONST_0, GOTO, ICONST_M1)),
		neq = n("neq", 2, new Code(INT_TYPE, "0>1>!N*!X|N*|X", IF_ICMPNE, ICONST_0, GOTO, ICONST_M1)),
		ls = n("ls", 2, new Code(INT_TYPE, "0>1>!L*!X|L*|X", IF_ICMPLT, ICONST_0, GOTO, ICONST_M1)),
		geq = n("geq", 2, new Code(INT_TYPE, "0>1>!G*!X|G*|X", IF_ICMPGE, ICONST_0, GOTO, ICONST_M1)),
		inc = n("inc", 1, new Code(INT_TYPE, ">**", ICONST_1, IADD)),
		dec = n("dec", 1, new Code(INT_TYPE, ">**", ICONST_1, ISUB)),
		neg = n("neg", 1, new Code(INT_TYPE, ">*", INEG)),
		abs = n("abs", 1, new Code(INT_TYPE, ">*!X*|X", DUP, IFGE, INEG)),
		add = n("add", 2, new Code(INT_TYPE, "0>1>*", IADD)),
		sub = n("sub", 2, new Code(INT_TYPE, "0>1>*", ISUB)),
		mul = n("mul", 2, new Code(INT_TYPE, "0>1>*", IMUL)),
		div = n("div", 2, new Code(INT_TYPE, "0>1>;java/lang/Math:floorDiv(II)I", INVOKESTATIC)),
		mod = n("mod", 2, new Code(INT_TYPE, "0>1>;java/lang/Math:floorMod(II)I", INVOKESTATIC)),
		bsl = n("bsl", 2, new Code(INT_TYPE, "0>1>*", ISHL)),
		bsr = n("bsr", 2, new Code(INT_TYPE, "0>1>*", ISHR)),
		usr = n("usr", 2, new Code(INT_TYPE, "0>1>*", IUSHR)),
		max = n("max", 2, new Code(INT_TYPE, "0>1>*!X*|X*", DUP2, IF_ICMPGE, SWAP, POP)),
		min = n("min", 2, new Code(INT_TYPE, "0>1>*!X*|X*", DUP2, IF_ICMPLE, SWAP, POP)),
		swt = n("swt", 3, new Code(INT_TYPE, "0>!A1>!X|A2>|X", IFNE, GOTO));

	public static final Code
		getInArr = new Code(Type.getType("[I"), "%t=:inputs [I", ALOAD, GETFIELD),
		getOutArr = new Code(Type.getType("[I"), "%t=:outputs [I", ALOAD, GETFIELD);

	static {
		TABS = new Category[4];
		Category c;
		TABS[0] = c = new Category("rs_ctr:io");
		INS_SET.add(0, c.add(in, out, read, write, i_cst));
		TABS[1] = c = new Category("rs_ctr:logic");
		INS_SET.add(16, c.add(not, or, nor, and, nand, xor, xnor));
		TABS[2] = c = new Category("rs_ctr:comp");
		INS_SET.add(32, c.add(not0, is0, nsgn, psgn, eq, neq, ls, geq));
		TABS[3] = c = new Category("rs_ctr:num");
		INS_SET.add(48, c.add(inc, dec, neg, abs, add, sub, mul, div, mod, max, min));
	}

	private static BasicType n(String name, int in, Code out) {
		return new BasicType(Combinator::new, name, 3, in < 3 ? in + 1 : in, in, out);
	}

	public final char[] OP_COSTS = new char[256];

	{
		OP_COSTS[0] = OP_COSTS[1] = OP_COSTS[2] = OP_COSTS[4] = 0x00_01;
		OP_COSTS[3] = 0x00_02;
		for (int i = 16; i < 32; i++)
			OP_COSTS[i] = 0x00_02;
		for (int i = 32; i < 48; i++)
			OP_COSTS[i] = 0x00_01;
		for (int i = 48; i < 64; i++)
			OP_COSTS[i] = 0x02_00;
	}

	public int getCost(GateType<?> t) {
		return OP_COSTS[id(t)];
	}

	@Override
	public boolean asBool() throws Error {return true;}

	@Override
	public Object value() {return this;}

	@Override
	public void put(IOperand idx, IOperand val) {
		String key = idx.toString();
		if (key.indexOf(':') < 0) key = Main.ID + ':' + key;
		Integer id = IDS.get(key);
		if (id == null || !(val instanceof Vector)) return;
		double[] v = ((Vector)val).value;
		OP_COSTS[id] = (char)((int)v[0] & 0xff | (int)v[1] << 8);
	}

}
