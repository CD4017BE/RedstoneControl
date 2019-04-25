package cd4017be.rs_ctr.processor.compiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import cd4017be.rs_ctr.api.circuitgraph.Array;
import cd4017be.rs_ctr.api.circuitgraph.Context;
import cd4017be.rs_ctr.api.circuitgraph.Endpoint;
import cd4017be.rs_ctr.api.circuitgraph.Operator;
import cd4017be.rs_ctr.api.circuitgraph.Output;
import cd4017be.rs_ctr.api.circuitgraph.Variable;
import cd4017be.rs_ctr.processor.Circuit;
import cd4017be.rs_ctr.processor.UnloadedCircuit;
import cd4017be.rs_ctr.processor.StateBuffer;

/**
 * @author CD4017BE
 *
 */
public class Compiler {

	public static final String
			THIS = UnloadedCircuit.name(new UUID(0, 0)),
			C_CIRCUIT = Type.getInternalName(Circuit.class),
			D_STATE_BUFFER = Type.getDescriptor(StateBuffer.class),
			C_STATE_BUFFER = Type.getInternalName(StateBuffer.class),
			D_STRING = Type.getDescriptor(String.class);

	public static byte[] compile(List<Endpoint> circuitGraph) {
		//sort stuff
		ArrayList<Variable> variables = new ArrayList<>();
		ArrayList<Operator> exits = new ArrayList<>();
		for (Endpoint ep : circuitGraph) {
			if (ep instanceof Variable)
				variables.add((Variable)ep);
			if (ep instanceof Output) {
				Operator op = ((Output)ep).write();
				if (op != null) exits.add(op);
			}
		}
		//compile
		ClassWriter cw = getHeader();
		addVariables(cw, variables);
		addTickImpl(cw, packAndOrder(exits));
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static List<Branch> packAndOrder(List<Operator> exits) {
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
				b = Branch.from(op);
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

	private static ClassWriter getHeader() {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw.newUTF8(THIS); //make sure class name is at constant pool index 1
		cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, THIS, null, C_CIRCUIT, null);
		return cw;
	}

	private static void addVariables(ClassWriter cw, List<Variable> variables) {
		//add fields
		for (Variable var : variables)
			cw.visitField(ACC_PRIVATE | (var instanceof Array ? ACC_FINAL : 0), var.name(), var.desc(), null, null);
		
		//implement constructor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, C_CIRCUIT, "<init>", "()V", false);
		for (Variable var : variables)
			if (var instanceof Array) {
				i_const(mv, ((Array)var).size());
				mv.visitIntInsn(NEWARRAY, ((Array)var).type());
				mv.visitFieldInsn(PUTFIELD, THIS, var.name(), var.desc());
			}
		mv.visitInsn(RETURN);
		mv.visitEnd();
		
		//implement getState()
		mv = cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "getState", "()" + D_STATE_BUFFER, null, null);
		mv.visitCode();
		mv.visitTypeInsn(NEW, C_STATE_BUFFER);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, C_STATE_BUFFER, "<init>", "()V", false);
		for (Variable var : variables) {
			String name = var.name(), desc = var.desc();
			mv.visitLdcInsn(name);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, THIS, name, desc);
			mv.visitMethodInsn(INVOKEVIRTUAL, C_STATE_BUFFER, "set", "(" + D_STRING + desc + ")" + D_STATE_BUFFER, false);
		}
		mv.visitInsn(ARETURN);
		mv.visitEnd();
		
		//implement setState()
		mv = cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "setState", "(" + D_STATE_BUFFER + ")V", null, null);
		mv.visitCode();
		for (Variable var : variables) {
			String name = var.name(), desc = var.desc();
			if (var instanceof Array) {
				mv.visitVarInsn(ALOAD, 1);
				mv.visitLdcInsn(name);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, THIS, name, desc);
				mv.visitMethodInsn(INVOKEVIRTUAL, C_STATE_BUFFER, "getArr", "(" + D_STRING + desc + ")V", false);
			} else {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitLdcInsn(name);
				mv.visitMethodInsn(INVOKEVIRTUAL, C_STATE_BUFFER, getter(desc), "(" + D_STRING + ")" + desc, false);
				mv.visitFieldInsn(PUTFIELD, THIS, name, desc);
			}
		}
		mv.visitInsn(RETURN);
		mv.visitEnd();
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

	private static void addTickImpl(ClassWriter cw, List<Branch> parts) {
		Context c = new Context();
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "tick", "()Z", null, null);
		mv.visitCode();
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, Context.DIRTY_IDX);
		mv.visitVarInsn(ALOAD, Context.THIS_IDX);
		mv.visitFieldInsn(GETFIELD, THIS, "inputs", "[I");
		mv.visitVarInsn(ASTORE, Context.IO_IDX);
		boolean processIn = true;
		for (Branch b : parts) {
			if (processIn && b.hasIO == Branch.IS_OUT) {
				mv.visitVarInsn(ALOAD, Context.THIS_IDX);
				mv.visitFieldInsn(GETFIELD, THIS, "outputs", "[I");
				mv.visitVarInsn(ASTORE, Context.IO_IDX);
				processIn = false;
			}
			b.compile(c).accept(mv);
		}
		mv.visitVarInsn(ILOAD, Context.DIRTY_IDX);
		mv.visitInsn(IRETURN);
		mv.visitEnd();
	}

	private static void i_const(MethodVisitor mv, int val) {
		if (val >= -1 && val <= 5)
			mv.visitInsn(ICONST_0 + val);
		else if (val >= Byte.MIN_VALUE && val < Byte.MAX_VALUE)
			mv.visitIntInsn(BIPUSH, val);
		else if (val >= Short.MIN_VALUE && val < Short.MAX_VALUE)
			mv.visitIntInsn(SIPUSH, val);
		else
			mv.visitLdcInsn(val);
	}

}
