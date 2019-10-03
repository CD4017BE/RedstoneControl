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
	public static final Parser PARSER = new Parser();

	public static ASMCode get(ResourceLocation loc) {
		ASMCode code = REGISTRY.get(loc);
		if (code != null) return code;
		InputStream is = IOUtils.getClassResource(loc, "/logic/nodes/", ".jasm");
		if (is == null) {
			Main.LOG.error("missing asm code {}", loc);
			return code;
		}
		try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			br.lines()
				.map((l)-> { //remove comments
					int i = l.indexOf('#');
					if (i >= 0) l = l.substring(0, i);
					return l.trim();
				}).filter((l)-> !l.isEmpty())
				.forEachOrdered(PARSER); //remove empty lines
			REGISTRY.put(loc, code = PARSER.build());
		} catch (IOException | IllegalArgumentException e) {
			Main.LOG.error("failed loading asm code " + loc, e);
		} finally {
			PARSER.clear();
		}
		return code;
	}

	final Insn[] instructions;
	final int locals, labels;
	public ASMCode extra;

	public ASMCode(Insn[] instructions, int locals, int labels) {
		this.instructions = instructions;
		this.locals = locals;
		this.labels = labels;
	}

	static final HashMap<String, Insn> TRANSLATION = new HashMap<>();
	static {
		TRANSLATION.put("CLR", new IntInsn(-6, 1));
		TRANSLATION.put("JOF", new IntInsn(-5, 3));
		TRANSLATION.put("JOT", new IntInsn(-4, 3));
		TRANSLATION.put("IN", new IntInsn(-3, 3));
		TRANSLATION.put("EVDEPS", new IntInsn(-2, 3));
		TRANSLATION.put("EVDEP", new IntInsn(-1, 3));
		put(0, Insn::new,
			"NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4",
			"ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0", "FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1"
		);
		TRANSLATION.put("BIPUSH", new IntInsn(16, 0));
		TRANSLATION.put("SIPUSH", new IntInsn(17, 0));
		TRANSLATION.put("LDC", new LdcInsn());
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

	public static class Parser implements Consumer<String> {

		private ASMCode main, jot, jof;
		private byte section = 0;
		private ArrayList<String> locals = new ArrayList<>(), labels = new ArrayList<>();
		private ArrayList<Insn> list = new ArrayList<>();

		private Parser() {}

		public int getLocal(String name) {
			if ("this".equals(name)) return -1;
			if ("mod".equals(name)) return -2;
			int i = locals.indexOf(name);
			if (i < 0) {
				i = locals.size();
				locals.add(name);
			}
			return i;
		}

		public int getLabel(String name) {
			if (section > 0 && "dst".equals(name)) return -1;
			int i = labels.indexOf(name);
			if (i < 0) {
				i = labels.size();
				labels.add(name);
			}
			return i;
		}

		@Override
		public void accept(String t) {
			String[] tokens = t.split("\\s+");
			String cmd = tokens[0];
			Insn ins = TRANSLATION.get(cmd.toUpperCase());
			int l = cmd.length();
			if (ins != null)
				ins = ins.parse(tokens, this);
			else if (l > 0 && cmd.charAt(l - 1) == ':') {
				cmd = cmd.substring(0, l - 1);
				if ("true".equals(cmd)) {
					pushState();
					section = 1;
					return;
				} else if ("false".equals(cmd)) {
					pushState();
					section = 2;
					return;
				}
				ins = new IntInsn(0, (byte)2, getLabel(cmd));
			} else {
				Insn.assertArgs(tokens, 1);
				ins = new DefLocal(getLocal(tokens[1]), IOUtils.getValidType(cmd));
			}
			list.add(ins);
		}

		private void pushState() {
			ASMCode code = new ASMCode(list.toArray(new Insn[list.size()]), locals.size(), labels.size());
			switch(section) {
			case 0: main = code; break;
			case 1: jot = code; break;
			case 2: jof = code; break;
			}
			list.clear();
			labels.clear();
			locals.clear();
		}

		public ASMCode build() {
			pushState();
			if (jot != null && jof != null) {
				jof.extra = jot;
				main.extra = jof;
			}
			return main;
		}

		public void clear() {
			main = jot = jof = null;
			section = 0;
			list.clear();
			labels.clear();
			locals.clear();
		}

	}

	public static class CompCont {

		final Context context;
		final Object[] args;
		final Dep[] inputs;
		final int[] locals;
		final Type[] types;
		final Label[] labels;
		final Label target;

		public CompCont(ASMCode code, Dep[] inputs, Object[] args, Context context, Label target) {
			this.inputs = inputs;
			this.args = args;
			this.context = context;
			if (code.locals > 0) {
				this.types = new Type[code.locals];
				this.locals = new int[code.locals];
				Arrays.fill(locals, -1);
			} else {
				this.types = null;
				this.locals = null;
			}
			if (code.labels > 0) {
				this.labels = new Label[code.labels];
				for (int i = 0; i < labels.length; i++)
					labels[i] = new Label();
			} else this.labels = null;
			this.target = target;
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
			if (locals != null)
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
			assertArgs(args, 0);
			return this;
		}

		public static void assertArgs(String[] args, int n) {
			if (args.length != n + 1)
				throw new IllegalArgumentException("expected opcode with " + n + " arguments but got: " + String.join(" ", args));
		}

		public void visit(MethodVisitor mv, CompCont cont) {
			mv.visitInsn(opcode);
		}

		@Override
		public String toString() {
			return "Insn " + opcode;
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
				assertArgs(args, 1);
				arg = Integer.parseInt(args[1]);
				break;
			case 1:
				assertArgs(args, 1);
				arg = cont.getLocal(args[1]);
				break;
			case 2:
				assertArgs(args, 1);
				arg = cont.getLabel(args[1]);
				break;
			case 3:
				if (opcode < -3) {
					assertArgs(args, 2);
					arg = cont.getLabel(args[2]) << 16;
				} else {
					assertArgs(args, 1);
					arg = 0;
				}
				arg |= Integer.parseInt(args[1]) & 0xffff;
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
				else {
					int l;
					if (arg < 0)
						l = arg == -1 ? Context.THIS_IDX : Context.DIRTY_IDX;
					else if (cont.locals == null || (l = cont.locals[arg]) < 0)
						throw new IllegalStateException("local variable " + arg + " for varInsn " + opcode + " not defined");
					mv.visitVarInsn(opcode, l);
				}
				break;
			case 2:
				if (opcode == 0) mv.visitLabel(cont.labels[arg]);
				else mv.visitJumpInsn(opcode, arg < 0 ? cont.target : cont.labels[arg]);
				break;
			case 3:
				Dep in = cont.inputs[arg & 0xffff];
				if (opcode < -3)
					in.compile(mv, cont.context, arg < 0 ? cont.target : cont.labels[arg >> 16], opcode == -4);
				else if (opcode == -3)
					in.compile(mv, cont.context);
				else
					in.ensureDependencyEvaluation(mv, cont.context, opcode == -2);
				break;
			}
		}

		private static final String[] TYPES = {"IntInsn ", "VarInsn ", "JumpInsn ", "Input "};
		@Override
		public String toString() {
			return TYPES[type] + opcode + " " + arg;
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
			if (opcode < Opcodes.INVOKEDYNAMIC) {
				assertArgs(args, 3);
				return new DescInsn(opcode, args[1], args[2], args[3]);
			} else {
				assertArgs(args, 1);
				return new DescInsn(opcode, null, null, args[1]);
			}
		}

		@Override
		public void visit(MethodVisitor mv, CompCont cont) {
			String owner = this.owner, name = this.name, desc = this.desc;
			if ("this".equals(owner)) owner = cont.context.compiler.C_THIS;
			else if ("super".equals(owner)) owner = cont.context.compiler.C_SUPER;
			if (name != null) name = replace(name, cont).toString();
			if (desc != null) desc = replace(desc, cont).toString();
			if (opcode < Opcodes.INVOKEVIRTUAL)
				mv.visitFieldInsn(opcode, owner, name, desc);
			else if (opcode < Opcodes.INVOKEDYNAMIC)
				mv.visitMethodInsn(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE);
			else
				mv.visitTypeInsn(opcode, desc);
		}

		@Override
		public String toString() {
			String s = "DescInsn " + opcode + " " + owner;
			return name != null ? s + "." + name + desc : s;
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
			assertArgs(args, 1);
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

		@Override
		public String toString() {
			return "LdcInsn " + val;
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

		@Override
		public String toString() {
			return type + " " + idx;
		}

	}

	static Object replace(String arg, CompCont cont) {
		if (arg.isEmpty()) return arg;
		char c = arg.charAt(0);
		if (c == '\\') return arg.substring(1);
		if (c != '$') return arg;
		return cont.args[Integer.parseInt(arg.substring(1))];
	}

}
