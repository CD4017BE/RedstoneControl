package cd4017be.rs_ctr.circuit.editor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.compile.Compiler;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.compile.Dep;
import cd4017be.rscpl.util.IOUtils;
import net.minecraft.util.ResourceLocation;
import scala.actors.threadpool.Arrays;

/**
 * 
 * @author cd4017be
 *
 */
public class ASMCode {

	private static final HashMap<ResourceLocation, ASMCode> REGISTRY = new HashMap<>();

	public static ASMCode get(ResourceLocation loc) {
		ASMCode code = REGISTRY.get(loc);
		if (code != null) return code;
		InputStream is = IOUtils.getClassResource(loc, "/gates/nodes/", ".jasm");
		if (is == null) {
			Main.LOG.error("missing asm code {}", loc);
			return code;
		}
		try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			Parser cont = new Parser();
			br.lines()
				.map((l)-> { //remove comments
					int i = l.indexOf('#');
					if (i >= 0) l = l.substring(0, i);
					return l.trim();
				}).filter((l)-> !l.isEmpty())
				.forEachOrdered(cont); //remove empty lines
			REGISTRY.put(loc, code = cont.build());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return code;
	}

	final Insn[] instructions;
	final int locals, labels, jof, jot;

	public ASMCode(Insn[] instructions, int locals, int labels, int jof, int jot) {
		this.instructions = instructions;
		this.locals = locals;
		this.labels = labels;
		this.jof = jof;
		this.jot = jot;
	}

	static final HashMap<String, Insn> TRANSLATION = new HashMap<>();
	static {
		TRANSLATION.put("CLR", new IntInsn(-6, 1));
		TRANSLATION.put("EVDEPS", new IntInsn(-5, 3));
		TRANSLATION.put("EVDEP", new IntInsn(-4, 3));
		TRANSLATION.put("JOF", new IntInsn(-3, 3));
		TRANSLATION.put("JOT", new IntInsn(-2, 3));
		TRANSLATION.put("IN", new IntInsn(-1, 3));
		put(0, Insn::new,
			"NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4",
			"ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0", "FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1"
		);
		TRANSLATION.put("BIPUSH", new IntInsn(16, 0));
		TRANSLATION.put("SIPUSH", new IntInsn(17, 0));
		put(21, i -> new IntInsn(i, 1), "ILOAD", "LLOAD", "FLOAD", "DLOAD", "ALOAD");
		put(46, Insn::new,
			"IALOAD", "LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD", "CALOAD", "SALOAD"
		);
		put(54, i -> new IntInsn(i, 1), "ISTORE", "LSTORE", "FSTORE", "DSTORE", "ASTORE");
		put(79, Insn::new,
			"IASTORE", "LASTORE", "FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE", "SASTORE",
			"POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1", "DUP2_X2", "SWAP",
			"IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB", "DSUB", 
			"IMUL", "LMUL", "FMUL", "DMUL", "IDIV", "LDIV", "FDIV", "DDIV",
			"IREM", "LREM", "FREM", "DREM", "INEG", "LNEG", "FNEG", "DNEG",
			"ISHL", "LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND",
			"IOR", "LOR", "IXOR", "LXOR", null, "I2L", "I2F", "I2D",
			"L2I", "L2F", "L2D", "F2I", "F2L", "F2D", "D2I", "D2L",
			"D2F", "I2B", "I2C", "I2S", "LCMP", "FCMPL", "FCMPG", "DCMPL", "DCMPG"
		);
		//TODO iinc 132
		put(153, i -> new IntInsn(i, 2),
			"IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE", "IF_ICMPEQ", "IF_ICMPNE",
			"IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO", "JSR"
		);
		TRANSLATION.put("RET", new IntInsn(169, 1));
		//TODO TABLESWITCH 170
		//TODO LOOKUPSWITCH 171
		put(172, Insn::new,
			"IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN", "RETURN"
		);
		put(178, DescInsn::new,
			"GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD", "INVOKEVIRTUAL", "INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE"
		);
		TRANSLATION.put("NEWARRAY", new IntInsn(188, 0));
		put(187, DescInsn::new,
			"NEW", null, "ANEWARRAY", null, null, "CHECKCAST", "INSTANCEOF"
		);
		put(190, Insn::new,
			"ARRAYLENGTH", "ATHROW", null, null, "MONITORENTER", "MONITOREXIT"
		);
		TRANSLATION.put("IFNULL", new IntInsn(198, 2));
		TRANSLATION.put("IFNONNULL", new IntInsn(199, 2));
	}

	private static void put(int start, IntFunction<Insn> instr, String... opcodes) {
		for (String s : opcodes)
			if (s != null)
				TRANSLATION.put(s, instr.apply(start++));
			else start++;
	}

	static class Parser implements Consumer<String> {

		ArrayList<String> locals = new ArrayList<>(), labels = new ArrayList<>();
		ArrayList<Insn> list = new ArrayList<>();
		int jot = Integer.MAX_VALUE, jof = Integer.MAX_VALUE;

		public int getLocal(String name) {
			int i = locals.indexOf(name);
			if (i < 0) {
				i = locals.size();
				locals.add(name);
			}
			return i;
		}

		public int getLabel(String name) {
			int i = labels.indexOf(name);
			if (i < 0) {
				i = labels.size();
				locals.add(name);
			}
			return i;
		}

		@Override
		public void accept(String t) {
			String[] tokens = t.split("\\s+");
			String cmd = tokens[0];
			Insn ins = TRANSLATION.get(cmd.toUpperCase());
			if (ins != null)
				ins = ins.parse(tokens, this);
			else if (cmd.charAt(cmd.length() - 1) == ':')
				ins = new IntInsn(0, (byte)2, getLabel(cmd.substring(0, cmd.length() - 1)));
			else
				ins = new DefLocal(getLocal(tokens[1]), Type.getType(cmd));
			list.add(ins);
		}

		public ASMCode build() {
			Insn[] ins = new Insn[list.size()];
			int e = Math.min(Math.min(jot, jof), list.size());
			int pt, pf;
			for (pt = 0; pt < e; pt++)
				ins[pt] = list.get(pt);
			pf = pt;
			if (jot < list.size()) {
				e = list.size();
				if (jof > jot && jof < e) e = jof;
				for (int i = jot; i < e; i++, pf++)
					ins[pf] = list.get(i);
			}
			if (jof < list.size()) {
				e = ins.length;
				for (int i = jof, j = pf; j < e; i++, j++)
					ins[j] = list.get(i);
			}
			return new ASMCode(ins, locals.size(), labels.size(), pt, pf);
		}

	}

	public static class CompCont {

		final Context context;
		final Object[] args;
		final Dep[] inputs;
		final int[] locals;
		final Type[] types;
		final Label[] labels;

		public CompCont(ASMCode code, Dep[] inputs, Object[] args, Context context) {
			this.inputs = inputs;
			this.args = args;
			this.context = context;
			this.types = new Type[code.locals];
			this.locals = new int[code.locals];
			Arrays.fill(locals, -1);
			this.labels = new Label[code.labels];
			for (int i = 0; i < labels.length; i++)
				labels[i] = new Label();
		}

		public void defineLocal(int i, Type type) {
			clearLocal(i);
			locals[i] = context.newLocal(type);
			types[i] = type;
		}

		public void clearLocal(int i) {
			int idx = locals[i];
			if (idx < 0) return;
			context.releaseLocal(idx, types[i]);
			locals[i] = -1;
			types[i] = null;
		}

		public void clear() {
			for (int i = 0; i < locals.length; i++)
				clearLocal(i);
		}
	}

	public static class Insn {

		final int opcode;

		public Insn(int opcode) {
			this.opcode = opcode;
		}

		public Insn parse(String[] args, Parser cont) {
			return this;
		}

		public void visit(MethodVisitor mv, CompCont cont) {
			mv.visitInsn(opcode);
		}

	}

	static class IntInsn extends Insn {

		final int arg;
		/**0:IntInsn, 1:VarInsn, 2:JumpInsn, 3:IN */
		final byte type;

		public IntInsn(int opcode, int type) {
			this(opcode, (byte)type, 0);
		}

		public IntInsn(int opcode, byte type, int arg) {
			super(opcode);
			this.type = type;
			this.arg = arg;
		}

		@Override
		public Insn parse(String[] args, Parser cont) {
			int arg;
			switch(type) {
			case 0:
				arg = Integer.parseInt(args[1]);
				break;
			case 1:
				arg = cont.getLocal(args[1]);
				break;
			case 2:
				arg = cont.getLabel(args[1]);
				break;
			case 3:
				arg = Integer.parseInt(args[1]) & 0xffff;
				if (opcode != -1)
					arg |= cont.getLabel(args[2]) << 16;
				break;
			default: return null;
			}
			return new IntInsn(opcode, type, arg);
		}

		@Override
		public void visit(MethodVisitor mv, CompCont cont) {
			switch(type) {
			case 0:
				mv.visitIntInsn(opcode, arg);
				break;
			case 1:
				if (opcode < 0) cont.clearLocal(arg);
				else mv.visitVarInsn(opcode, cont.locals[arg]);
				break;
			case 2:
				if (opcode == 0) mv.visitLabel(cont.labels[arg]);
				else mv.visitJumpInsn(opcode, cont.labels[arg]);
				break;
			case 3:
				Dep in = cont.inputs[arg & 0xffff];
				if (opcode == -1) in.compile(mv, cont.context);
				else if (opcode <= -4) in.ensureDependencyEvaluation(mv, cont.context, opcode < -4);
				else in.compile(mv, cont.context, cont.labels[arg >> 16], opcode == -2);
				break;
			}
		}

	}

	static class DescInsn extends Insn {

		final String owner, name, desc;

		public DescInsn(int opcode) {
			this(opcode, null, null, null);
		}

		public DescInsn(int opcode, String owner, String name, String desc) {
			super(opcode);
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		@Override
		public Insn parse(String[] args, Parser cont) {
			return new DescInsn(opcode, args[1], args[2], args[3]);
		}

		@Override
		public void visit(MethodVisitor mv, CompCont cont) {
			if (opcode < Opcodes.INVOKEVIRTUAL)
				mv.visitFieldInsn(opcode, owner, name, desc);
			else if (opcode < Opcodes.INVOKEDYNAMIC)
				mv.visitMethodInsn(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE);
			else
				mv.visitTypeInsn(opcode, owner);
		}

	}

	static class LdcInsn extends Insn {

		final Object val;

		public LdcInsn() {
			this(null);
		}

		public LdcInsn(Object val) {
			super(18);
			this.val = val;
		}

		@Override
		public Insn parse(String[] args, Parser cont) {
			String arg = args[1];
			Object val;
			switch(arg.charAt(0)) {
			case '$':
				val = Byte.parseByte(arg.substring(1));
				break;
			case '\'':
			case '"':
				val = arg.substring(1, arg.length() - 1);
				break;
			case 'F':
				val = Float.parseFloat(arg.substring(1));
				break;
			case 'D':
				val = Double.parseDouble(arg.substring(1));
				break;
			case 'L':
				val = Long.parseLong(arg.substring(1));
				break;
			default:
				int v = Integer.parseInt(arg);
				if (v >= -1 && v <= 5)
					return new Insn(3 + v);
				if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE)
					return new IntInsn(16, (byte)0, v);
				if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE)
					return new IntInsn(17, (byte)0, v);
				val = v;
			}
			return new LdcInsn(val);
		}

		@Override
		public void visit(MethodVisitor mv, CompCont cont) {
			if (val instanceof Byte) {
				Object v = cont.args[(Byte)val & 0xff];
				if (v instanceof Integer)
					Compiler.i_const(mv, (Integer)v);
				else mv.visitLdcInsn(v);
			} else mv.visitLdcInsn(val);
		}

	}

	static class DefLocal extends Insn {

		final int idx;
		final Type type;

		public DefLocal(int idx, Type type) {
			super(0);
			this.idx = idx;
			this.type = type;
		}

		@Override
		public void visit(MethodVisitor mv, CompCont cont) {
			cont.defineLocal(idx, type);
		}

	}

}
