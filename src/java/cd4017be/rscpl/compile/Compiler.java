package cd4017be.rscpl.compile;

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
import cd4017be.rscpl.graph.IArrayVar;
import cd4017be.rscpl.graph.IEndpoint;
import cd4017be.rscpl.graph.IVariable;
import cd4017be.rscpl.graph.IReadVar;
import cd4017be.rscpl.graph.IWriteVar;
import cd4017be.rscpl.util.IStateSerializable;
import cd4017be.rscpl.util.StateBuffer;

/**
 * 
 * @author CD4017BE
 */
public abstract class Compiler<P extends CompiledProgram> implements MethodCompiler {

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
	public P compile(Collection<Gate> gates) throws InvalidSchematicException {
		List<IEndpoint> ends = new ArrayList<>();
		Map<String, IVariable> variables = new HashMap<>();
		checkAndSort(gates, ends, variables);
		
		P p = newProgram(gates);
		ClassWriter cw = getHeader();
		addVariables(cw, p.getState(), variables.values());
		
		List<Node> nodes = new ArrayList<>(ends.size());
		for (IEndpoint end : ends) nodes.add(end.getEndNode());
		MethodCompiler.addMethod(cw, this, nodes);
		
		cw.visitEnd();
		p.setCode(cw.toByteArray());
		return p;
	}

	protected abstract P newProgram(Collection<Gate> gatesIn) throws InvalidSchematicException;

	protected ClassWriter getHeader() {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw.newUTF8(C_THIS); //make sure class name is at constant pool index 1
		cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, C_THIS, null, C_SUPER, C_INTERFACES);
		return cw;
	}

	protected void addVariables(ClassWriter cw, StateBuffer state, Collection<IVariable> variables) throws InvalidSchematicException {
		//add fields
		for (IVariable var : variables)
			cw.visitField(ACC_PRIVATE | (var instanceof IArrayVar ? ACC_FINAL : 0), var.name(), var.type().getDescriptor(), null, null);
		
		//implement constructor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, C_SUPER, "<init>", "()V", false);
		boolean hasArrays = false;
		for (IVariable var : variables)
			if (var instanceof IArrayVar) {
				mv.visitVarInsn(ALOAD, 0);
				i_const(mv, ((IArrayVar)var).size());
				mv.visitIntInsn(NEWARRAY, var.type().getElementType().getSort());
				mv.visitFieldInsn(PUTFIELD, C_THIS, var.name(), var.type().getDescriptor());
				hasArrays = true;
			}
		mv.visitInsn(RETURN);
		mv.visitMaxs(hasArrays ? 2 : 1, 1);
		mv.visitEnd();
		
		if (!stateSerialize) return;
		for(IVariable var : variables)
			if (var instanceof IReadVar)
				((IReadVar)var).initState(state);
		
		//implement getState()
		mv = cw.visitMethod(ACC_PUBLIC, "getState", "()" + D_STATE_BUFFER, null, null);
		mv.visitCode();
		mv.visitTypeInsn(NEW, C_STATE_BUFFER);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, C_STATE_BUFFER, "<init>", "()V", false);
		for (IVariable var : variables) {
			String name = var.name(), desc = var.type().getDescriptor();
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
		for (IVariable var : variables) {
			String name = var.name(), desc = var.type().getDescriptor();
			if (var instanceof IArrayVar) {
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

	protected static void checkAndSort(Collection<Gate> gatesIn, List<IEndpoint> endsOut, Map<String, IVariable> variablesOut) throws InvalidSchematicException {
		ArrayList<IReadVar> reads = new ArrayList<>();
		for (Gate g : gatesIn) {
			if (g == null) continue;
			g.check = 0;
			addOperator(g, variablesOut, reads, endsOut);
		}
		for (IReadVar r : reads) {
			IVariable op = variablesOut.put(r.name(), r);
			if (op instanceof IWriteVar) ((IWriteVar)op).link(r);
			else if (op != null)
				throw new InvalidSchematicException(READ_CONFLICT, (Gate)r, 0);
		}
		for (IEndpoint op : endsOut) ((Gate)op).checkValid();
		Collections.sort(endsOut, (a, b)-> {
			Gate ga = (Gate)a, gb = (Gate)b;
			return ga.type.name.compareTo(gb.type.name);
		});
	}

	protected static void addOperator(Gate op, Map<String, IVariable> writes, List<IReadVar> reads, List<IEndpoint> ends) throws InvalidSchematicException {
		if (op == null) return;
		if (op instanceof IWriteVar) {
			IWriteVar w = (IWriteVar)op;
			checkName(w);
			if (writes.put(w.name(), w) != null)
				throw new InvalidSchematicException(WRITE_CONFLICT, op, 0);
			w.link(null);
		}
		if (op instanceof IReadVar) {
			IReadVar r = (IReadVar)op;
			checkName(r);
			reads.add(r);
		}
		if (op instanceof IEndpoint) ends.add((IEndpoint)op);
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

	public static void checkName(IVariable op) throws InvalidSchematicException {
		valid: {
			String name = op.name();
			if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0)))
				break valid;
			for (int i = name.length() - 1; i > 0; i--)
				if (!Character.isJavaIdentifierPart(name.charAt(i)))
					break valid;
			return;
		}
		throw new InvalidSchematicException(INVALID_LABEL, (Gate)op, 0);
	}

}
