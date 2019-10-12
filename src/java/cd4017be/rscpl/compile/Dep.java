package cd4017be.rscpl.compile;

import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

/**
 * A dependency of a Programm {@link Node}, in other words a piece of code that needs to run before parts of the {@link #dst} Node can execute.<br>
 * With {@link #type} = {@link Type#VOID_TYPE} representing a pure programm state dependency without an actual value passed over.
 * @author cd4017be
 */
public class Dep implements Comparable<Dep> {

	public final Node src, dst;
	public final Type type;

	Dep(Node src, Node dst, Type type) {
		this.src = src;
		this.dst = dst;
		this.type = type;
		src.users.add(this);
		src.commonUser = null;
	}

	/**
	 * compile this input value to be on top of the operand stack
	 * @param mv the method being assembled
	 * @param context handler for local variables
	 * @see #compile(MethodVisitor, Context, Label, boolean)
	 * @see #ensureDependencyEvaluation(MethodVisitor, Context, boolean)
	 */
	public void compile(MethodVisitor mv, Context context) {
		NodeCompiler code = src.code;
		Type st = code.getOutType();
		if (type != Type.VOID_TYPE) {
			if (src.localIdx >= 0) {
				mv.visitVarInsn(st.getOpcode(ILOAD), src.localIdx);
				if (--src.remUses <= 0)
					context.releaseLocal(src.localIdx, st);
			} else {
				code.compile(src.deps, src.param, mv, context);
				if (src.getNumOfUsers() > 1) {
					int i = src.localIdx = context.newLocal(st);
					mv.visitVarInsn(st.getOpcode(ISTORE), i);
					mv.visitVarInsn(st.getOpcode(ILOAD), i);
				} else src.localIdx = Integer.MAX_VALUE;
				src.remUses--;
			}
			convert(st, type, mv);
			return;
		}
		if (src.localIdx >= 0) return;
		code.compile(src.deps, src.param, mv, context);
		if (st == Type.VOID_TYPE) {
			src.localIdx = Integer.MAX_VALUE;
			return;
		}
		if (src.getNumOfUsers() == 0) {
			src.localIdx = Integer.MAX_VALUE;
			mv.visitInsn(st.getSize() == 1 ? POP : POP2);
			return;
		}
		mv.visitVarInsn(st.getOpcode(ISTORE), src.localIdx = context.newLocal(st));
	}

	/**
	 * compile this boolean input value as conditional jump to the given target label
	 * @param mv the method being assembled
	 * @param context handler for local variables
	 * @param target the label to jump to
	 * @param cond the value to jump on
	 */
	public void compile(MethodVisitor mv, Context context, Label target, boolean cond) {
		if (type.getSort() > Type.INT) throw new IllegalStateException();
		NodeCompiler code = src.code;
		Type st = code.getOutType();
		if (src.localIdx >= 0) {
			mv.visitVarInsn(st.getOpcode(ILOAD), src.localIdx);
			if (--src.remUses <= 0)
				context.releaseLocal(src.localIdx, st);
		} else if (src.getNumOfUsers() > 1) {
			code.compile(src.deps, src.param, mv, context);
			int i = src.localIdx = context.newLocal(st);
			mv.visitVarInsn(st.getOpcode(ISTORE), i);
			mv.visitVarInsn(st.getOpcode(ILOAD), i);
			src.remUses--;
		} else {
			src.localIdx = Integer.MAX_VALUE;
			if (code instanceof NodeCompiler.Bool) {
				((NodeCompiler.Bool)code).compile(src.deps, src.param, mv, context, target, cond);
				return;
			}
			code.compile(src.deps, src.param, mv, context);
		}
		convert(st, type, mv);
		mv.visitJumpInsn(cond ? IFNE : IFEQ, target);
	}

	/**
	 * When this input is only evaluated conditionally
	 * then call this beforhand to ensure other parts of the programm
	 * that might also depend on values further down will have them evaluated.
	 * @param mv the method being assembled
	 * @param context handler for local variables
	 * @param needsOwn whether following inputs of the {@link #dst} Node may need them evaluated as well.
	 */
	public void ensureDependencyEvaluation(MethodVisitor mv, Context context, boolean needsOwn) {
		recCompDeps(src, mv, context, needsOwn ? dst.order - 1 : dst.order, new HashSet<>());
	}

	private void recCompDeps(Node src, MethodVisitor mv, Context context, int order, Set<Node> visited) {
		if (src.localIdx >= 0) return;
		if (src.users.size() > 1) {
			if (src.getCommonUser().order >= order) {
				new Dep(src, dst, Type.VOID_TYPE).compile(mv, context);
				return;
			}
			if (!visited.add(src)) return;
		}
		for (Dep d : src.deps)
			if (d != null)
				recCompDeps(d.src, mv, context, order, visited);
	}

	@Override
	public int compareTo(Dep o) {
		if (type == Type.BOOLEAN_TYPE)
			if (o.type != Type.BOOLEAN_TYPE) return -1;
			else return src.order - o.src.order;
		if (o.type == Type.BOOLEAN_TYPE) return 1;
		return o.src.order - src.order;
	}

	public static boolean canConvert(Type from, Type to, boolean strict) {
		if (to == Type.VOID_TYPE) return true;
		if (from == Type.VOID_TYPE) return false;
		if (from.equals(to)) return true;
		int st = from.getSort(), dt = to.getSort();
		if (strict && st > dt) return false;
		if (st < Type.ARRAY && dt < Type.ARRAY) return true;
		return false;
	}

	/**
	 * Performs a primitive type conversion of a value on the operand stack
	 * @param from input type
	 * @param to output type
	 * @param mv the method being assembled
	 */
	public static void convert(Type from, Type to, MethodVisitor mv) {
		int st = from.getSort(), dt = to.getSort();
		if (st == dt || st >= Type.ARRAY || dt >= Type.ARRAY) return;
		if (dt == Type.BOOLEAN)
			switch(st) {
			case Type.FLOAT: mv.visitInsn(FCMPG); return;
			case Type.LONG: mv.visitInsn(LCMP); return;
			case Type.DOUBLE: mv.visitInsn(DCMPG); return;
			default: return;
			}
		int op;
		if (st < Type.INT) st = Type.INT;
		else if (st > Type.INT && dt <= Type.INT) {
			switch(st) {
			case Type.FLOAT: op = F2I; break;
			case Type.LONG: op = L2I; break;
			case Type.DOUBLE: op = D2I; break;
			default: return;
			}
			mv.visitInsn(op);
			st = Type.INT;
		}
		if (st == dt) return;
		switch((st << 2) + dt) {
		case (Type.INT << 2) + Type.CHAR: op = I2C; break;
		case (Type.INT << 2) + Type.BYTE: op = I2B; break;
		case (Type.INT << 2) + Type.SHORT: op = I2S; break;
		case (Type.INT << 2) + Type.FLOAT: op = I2F; break;
		case (Type.INT << 2) + Type.LONG: op = I2L; break;
		case (Type.INT << 2) + Type.DOUBLE: op = I2D; break;
		case (Type.FLOAT << 2) + Type.LONG: op = F2L; break;
		case (Type.FLOAT << 2) + Type.DOUBLE: op = F2D; break;
		case (Type.LONG << 2) + Type.FLOAT: op = L2F; break;
		case (Type.LONG << 2) + Type.DOUBLE: op = L2D; break;
		case (Type.DOUBLE << 2) + Type.FLOAT: op = D2F; break;
		case (Type.DOUBLE << 2) + Type.LONG: op = D2L; break;
		default: return;
		}
		mv.visitInsn(op);
	}

}
