package cd4017be.rs_ctr.circuit.editor;

import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;

import org.objectweb.asm.Type;

import cd4017be.lib.script.obj.Error;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Vector;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.circuit.gates.ConstNum;
import cd4017be.rs_ctr.circuit.gates.Input;
import cd4017be.rs_ctr.circuit.gates.Output;
import cd4017be.rs_ctr.circuit.gates.ReadVar;
import cd4017be.rs_ctr.circuit.gates.WriteVar;
import cd4017be.rscpl.compile.NodeCompiler;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.editor.InstructionSet;
import cd4017be.rscpl.gui.Category;

import static org.objectweb.asm.Opcodes.*;
import static cd4017be.rs_ctr.circuit.editor.Code.bytes;

/**
 * @author CD4017BE
 *
 */
public class CircuitInstructionSet extends InstructionSet implements IOperand {

	public static final CircuitInstructionSet INS_SET = new CircuitInstructionSet();
	public static Category[] TABS;

	public static final Type INT_ARR_TYPE = Type.getType("[I");

	public static final BasicType
		in = new BasicType(Input::new, "in", 1, 2, 0, new Code(INT_TYPE, ">$*", bytes(IALOAD), INT_ARR_TYPE)),
		out = new BasicType(Output::new, "out", 1, 2, 1),
		read = new BasicType(ReadVar::new, "read", 6, 2, 0, new ReadVar.Compiler(INT_TYPE)),
		write = new BasicType(WriteVar::new, "write", 6, 2, 1, new Code(VOID_TYPE, "%t>=: I", bytes(ALOAD, PUTFIELD)), new Code(VOID_TYPE, ">1>!E %t>=: I %m**%m |E", bytes(IF_ICMPEQ, ALOAD, PUTFIELD, ILOAD, ICONST_1, IOR, ISTORE))),
		i_cst = new BasicType(ConstNum::new, "i_cst", 6, 2, 0, new Code(INT_TYPE, "$", null)),
		not = n("not", 1, new Code(INT_TYPE, ">**", bytes(ICONST_M1, IXOR))),
		or  = n("or" , 2, new Code(INT_TYPE, "0>1>*", bytes(IOR)).sortInputs(0)),
		nor = n("nor", 2, new Code(INT_TYPE, "0>1>3*", bytes(IOR, ICONST_M1, IXOR)).sortInputs(0)),
		and = n("and", 2, new Code(INT_TYPE, "0>1>*", bytes(IAND)).sortInputs(0)),
		nand= n("nand",2, new Code(INT_TYPE, "0>1>3*", bytes(IAND, ICONST_M1, IXOR)).sortInputs(0)),
		xor = n("xor", 2, new Code(INT_TYPE, "0>1>*", bytes(IXOR)).sortInputs(0)),
		xnor= n("xnor",2, new Code(INT_TYPE, "0>1>3*", bytes(IXOR, ICONST_M1, IXOR)).sortInputs(0)),
		not0= n("not0",1, new Code(INT_TYPE, ">!N*!X|N*|X", bytes(IFNE, ICONST_0, GOTO, ICONST_M1))),
		is0 = n("is0", 1, new Code(INT_TYPE, ">!E*!X|E*|X", bytes(IFEQ, ICONST_0, GOTO, ICONST_M1))),
		nsgn= n("nsgn",1, new Code(INT_TYPE, ">!L*!X|L*|X", bytes(IFLT, ICONST_0, GOTO, ICONST_M1))),
		psgn= n("psgn",1, new Code(INT_TYPE, ">!G*!X|G*|X", bytes(IFGE, ICONST_0, GOTO, ICONST_M1))),
		eq  = n("eq" , 2, new Code(INT_TYPE, "0>1>!E*!X|E*|X", bytes(IF_ICMPEQ, ICONST_0, GOTO, ICONST_M1)).sortInputs(0)),
		neq = n("neq", 2, new Code(INT_TYPE, "0>1>!N*!X|N*|X", bytes(IF_ICMPNE, ICONST_0, GOTO, ICONST_M1)).sortInputs(0)),
		ls  = n("ls" , 2, new Code(INT_TYPE, "0>1>!L*!X|L*|X", bytes(IF_ICMPLT, ICONST_0, GOTO, ICONST_M1))),
		geq = n("geq", 2, new Code(INT_TYPE, "0>1>!G*!X|G*|X", bytes(IF_ICMPGE, ICONST_0, GOTO, ICONST_M1))),
		inc = n("inc", 1, new Code(INT_TYPE, ">**", bytes(ICONST_1, IADD))),
		dec = n("dec", 1, new Code(INT_TYPE, ">**", bytes(ICONST_1, ISUB))),
		neg = n("neg", 1, new Code(INT_TYPE, ">*", bytes(INEG))),
		abs = n("abs", 1, new Code(INT_TYPE, ">*!X*|X",bytes(DUP, IFGE, INEG))),
		add = n("add", 2, new Code(INT_TYPE, "0>1>*", bytes(IADD)).sortInputs(0)),
		sub = n("sub", 2, new Code(INT_TYPE, "0>1>*", bytes(ISUB))),
		mul = n("mul", 2, new Code(INT_TYPE, "0>1>*", bytes(IMUL)).sortInputs(0)),
		div = n("div", 2, new Code(INT_TYPE, "0>1>;java/lang/Math:floorDiv(II)I", bytes(INVOKESTATIC))),
		mod = n("mod", 2, new Code(INT_TYPE, "0>1>;java/lang/Math:floorMod(II)I", bytes(INVOKESTATIC))),
		bsl = n("bsl", 2, new Code(INT_TYPE, "0>1>*", bytes(ISHL))),
		bsr = n("bsr", 2, new Code(INT_TYPE, "0>1>*", bytes(ISHR))),
		usr = n("usr", 2, new Code(INT_TYPE, "0>1>*", bytes(IUSHR))),
		max = n("max", 2, new Code(INT_TYPE, "0>1>*!X*|X*", bytes(DUP2, IF_ICMPGE, SWAP, POP)).sortInputs(0)),
		min = n("min", 2, new Code(INT_TYPE, "0>1>*!X*|X*", bytes(DUP2, IF_ICMPLE, SWAP, POP)).sortInputs(0)),
		swt = n("swt", 3, new Code(INT_TYPE, "0>!A1>!X|A2>|X", bytes(IFNE, GOTO)));

	public static final NodeCompiler
		getInArr = new Code(INT_ARR_TYPE, "%t=:inputs [I", bytes(ALOAD, GETFIELD)),
		getOutArr = new Code(INT_ARR_TYPE, "%t=:outputs [I", bytes(ALOAD, GETFIELD)),
		outCode = new Code(VOID_TYPE, "2>0>$*!E 1>$3>* %m1$*%m |E", bytes(IALOAD, IF_ICMPEQ, IASTORE, ILOAD, IOR, ISTORE), INT_ARR_TYPE, INT_ARR_TYPE);

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
		return new BasicType(Gate::new, name, 3, in < 3 ? in + 1 : in, in, out);
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

	public int getCost(GateType t) {
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
