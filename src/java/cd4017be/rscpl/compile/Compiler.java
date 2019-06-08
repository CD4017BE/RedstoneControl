package cd4017be.rscpl.compile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;
import static cd4017be.rscpl.editor.InvalidSchematicException.*;
import cd4017be.rscpl.graph.ArrayVar;
import cd4017be.rscpl.graph.NamedOp;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.ReadOp;
import cd4017be.rscpl.graph.WriteOp;
import cd4017be.rscpl.util.IStateSerializable;
import cd4017be.rscpl.util.StateBuffer;

/**
 * 
 * @author CD4017BE
 */
public abstract class Compiler<P extends CompiledProgram> {

	public static final String
			D_STATE_BUFFER = Type.getDescriptor(StateBuffer.class),
			C_STATE_BUFFER = Type.getInternalName(StateBuffer.class),
			D_STRING = Type.getDescriptor(String.class);
	public final String C_SUPER;
	public final String[] C_INTERFACES;
	public String C_THIS;
	protected final boolean stateSerialize;

	/**
	 * @param extend super class
	 * @param implement interfaces
	 */
	public Compiler(Class<?> extend, Class<?>... implement) {
		C_SUPER = Type.getInternalName(extend);
		C_INTERFACES = new String[implement.length];
		boolean ss = IStateSerializable.class.isAssignableFrom(extend);
		for (int i = 0; i < implement.length; i++) {
			Class<?> c = implement[i];
			C_INTERFACES[i] = Type.getInternalName(c);
			ss |= IStateSerializable.class.isAssignableFrom(c);
		}
		this.stateSerialize = ss;
	}

	/**
	 * @param c_name new output class name
	 * @return this
	 */
	public Compiler<P> setName(String c_name) {
		this.C_THIS = c_name.replace('.', '/');
		return this;
	}

	/**
	 * @param gates all gates in the program graph
	 * @return the compiled program
	 * @throws InvalidSchematicException if a compilation error occurs
	 */
	public P compile(Collection<Gate<?>> gates) throws InvalidSchematicException {
		List<Operator> ends = new ArrayList<>();
		Map<String, NamedOp> variables = new HashMap<>();
		checkAndSort(gates, ends, variables);
		
		P p = newProgram(gates);
		ClassWriter cw = getHeader();
		addVariables(cw, p.getState(), variables.values());
		addMain(p, cw, packAndArrange(ends));
		cw.visitEnd();
		p.setCode(cw.toByteArray());
		return p;
	}

	protected abstract P newProgram(Collection<Gate<?>> gatesIn) throws InvalidSchematicException;

	protected abstract void addMain(P program, ClassWriter cw, List<Branch> parts) throws InvalidSchematicException;

	protected ClassWriter getHeader() {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw.newUTF8(C_THIS); //make sure class name is at constant pool index 1
		cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, C_THIS, null, C_SUPER, C_INTERFACES);
		return cw;
	}

	protected void addVariables(ClassWriter cw, StateBuffer state, Collection<NamedOp> variables) throws InvalidSchematicException {
		//add fields
		for (NamedOp var : variables)
			cw.visitField(ACC_PRIVATE | (var instanceof ArrayVar ? ACC_FINAL : 0), var.name(), var.outType().getDescriptor(), null, null);
		
		//implement constructor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, C_SUPER, "<init>", "()V", false);
		boolean hasArrays = false;
		for (NamedOp var : variables)
			if (var instanceof ArrayVar) {
				mv.visitVarInsn(ALOAD, 0);
				i_const(mv, ((ArrayVar)var).size());
				mv.visitIntInsn(NEWARRAY, var.outType().getElementType().getSort());
				mv.visitFieldInsn(PUTFIELD, C_THIS, var.name(), var.outType().getDescriptor());
				hasArrays = true;
			}
		mv.visitInsn(RETURN);
		mv.visitMaxs(hasArrays ? 2 : 1, 1);
		mv.visitEnd();
		
		if (!stateSerialize) return;
		for(NamedOp var : variables)
			if (var instanceof ReadOp)
				((ReadOp)var).initState(state);
		
		//implement getState()
		mv = cw.visitMethod(ACC_PUBLIC, "getState", "()" + D_STATE_BUFFER, null, null);
		mv.visitCode();
		mv.visitTypeInsn(NEW, C_STATE_BUFFER);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, C_STATE_BUFFER, "<init>", "()V", false);
		for (NamedOp var : variables) {
			String name = var.name(), desc = var.outType().getDescriptor();
			mv.visitLdcInsn(name);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, C_THIS, name, desc);
			mv.visitMethodInsn(INVOKEVIRTUAL, C_STATE_BUFFER, "set", "(" + D_STRING + desc + ")" + D_STATE_BUFFER, false);
		}
		mv.visitInsn(ARETURN);
		mv.visitMaxs(3, 1);
		mv.visitEnd();
		
		//implement setState()
		mv = cw.visitMethod(ACC_PUBLIC, "setState", "(" + D_STATE_BUFFER + ")V", null, null);
		mv.visitCode();
		for (NamedOp var : variables) {
			String name = var.name(), desc = var.outType().getDescriptor();
			if (var instanceof ArrayVar) {
				mv.visitVarInsn(ALOAD, 1);
				mv.visitLdcInsn(name);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, C_THIS, name, desc);
				mv.visitMethodInsn(INVOKEVIRTUAL, C_STATE_BUFFER, "getArr", "(" + D_STRING + desc + ")V", false);
			} else {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKEVIRTUAL, C_STATE_BUFFER, getter(desc), "(" + D_STRING + ")" + desc, false);
				mv.visitFieldInsn(PUTFIELD, C_THIS, name, desc);
			}
		}
		mv.visitInsn(RETURN);
		mv.visitMaxs(3, 2);
		mv.visitEnd();
	}

	protected static void checkAndSort(Collection<Gate<?>> gatesIn, List<Operator> endsOut, Map<String, NamedOp> variablesOut) throws InvalidSchematicException {
		ArrayList<ReadOp> reads = new ArrayList<>();
		for (Gate<?> g : gatesIn) {
			if (g == null) continue;
			g.check = 0;
			g.restoreInputs();
			int i = g.outputCount();
			do addOperator(g.getOutput(--i), variablesOut, reads, endsOut);
			while(i > 0); //there is always at least one output pin, even if outputCount() == 0 (invisible pin).
		}
		for (ReadOp r : reads) {
			NamedOp op = variablesOut.put(r.name(), r);
			if (op instanceof WriteOp) ((WriteOp)op).link(r);
			else if (op != null)
				throw new InvalidSchematicException(READ_CONFLICT, r.getGate(), r.getPin());
		}
		for (Operator op : endsOut) op.getGate().checkValid();
		Collections.sort(endsOut, (a, b)-> {
			Gate<?> ga = a.getGate(), gb = b.getGate();
			int i = ga.type.name.compareTo(gb.type.name);
			if (i != 0) return i;
			i = a.getPin() - b.getPin();
			if (i != 0) return i;
			return ga.label.compareTo(gb.label);
		});
	}

	protected static void addOperator(Operator op, Map<String, NamedOp> writes, List<ReadOp> reads, List<Operator> ends) throws InvalidSchematicException {
		if (op == null) return;
		if (op instanceof WriteOp) {
			WriteOp w = (WriteOp)op;
			checkName(w);
			if (writes.put(w.name(), w) != null)
				throw new InvalidSchematicException(WRITE_CONFLICT, w.getGate(), w.getPin());
		}
		if (op instanceof ReadOp) {
			ReadOp r = (ReadOp)op;
			checkName(r);
			reads.add(r);
		}
		if (op.hasSideEffects() && op.receivers().isEmpty()) ends.add(op);
	}

	public static List<Branch> packAndArrange(List<Operator> exits) {
		HashMap<Operator, Branch> provided = new HashMap<>();
		ArrayDeque<LoadOp> required = new ArrayDeque<>();
		for (Operator op : exits) {
			if (!op.receivers().isEmpty()) continue;
			Branch b = Branch.from(op);
			provided.put(op, b);
			for (LoadOp o : b.inputs)
				required.add(o);
		}
		while (!required.isEmpty()) {
			LoadOp load = required.remove();
			Operator op = load.getInput(0);
			Branch b = provided.get(op);
			if (b == null) {
				if (op instanceof Branch) b = (Branch)op;
				else b = Branch.from(op);
				provided.put(op, b);
				for (LoadOp o : b.inputs)
					required.add(o);
			}
			load.setInput(0, b);
		}
		ArrayList<Branch> list = new ArrayList<>(provided.values());
		Collections.sort(list);
		return list;
	}

	private static String getter(String desc) {
		switch(desc.charAt(0)) {
		case 'B': return "getByte";
		case 'S': return "getShort";
		case 'I': return "getInt";
		case 'F': return "getFloat";
		case 'J': return "getLong";
		case 'D': return "getDouble";
		default: return null;
		}
	}

	public static void i_const(MethodVisitor mv, int val) {
		if (val >= -1 && val <= 5)
			mv.visitInsn(ICONST_0 + val);
		else if (val >= Byte.MIN_VALUE && val < Byte.MAX_VALUE)
			mv.visitIntInsn(BIPUSH, val);
		else if (val >= Short.MIN_VALUE && val < Short.MAX_VALUE)
			mv.visitIntInsn(SIPUSH, val);
		else
			mv.visitLdcInsn(val);
	}

	public static void checkName(NamedOp op) throws InvalidSchematicException {
		valid: {
			String name = op.name();
			if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0)))
				break valid;
			for (int i = name.length() - 1; i > 0; i--)
				if (!Character.isJavaIdentifierPart(name.charAt(i)))
					break valid;
			return;
		}
		throw new InvalidSchematicException(INVALID_LABEL, op.getGate(), op.getPin());
	}

}
