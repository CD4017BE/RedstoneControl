package cd4017be.rs_ctr.circuit.editor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.compile.Dep;
import cd4017be.rscpl.compile.NodeCompiler;

/**
 * 
 * @author cd4017be
 */
public class GetFieldNode implements NodeCompiler {

	public final Type type;

	public GetFieldNode(Type type) {
		this.type = type;
	}

	@Override
	public Type getInType(int i) {
		return null;
	}

	@Override
	public Type getOutType() {
		return type;
	}

	@Override
	public void compile(Dep[] inputs, Object param, MethodVisitor mv, Context context) {
		mv.visitVarInsn(Opcodes.ALOAD, Context.THIS_IDX);
		mv.visitFieldInsn(Opcodes.GETFIELD, context.compiler.C_THIS, (String)param, type.getDescriptor());
	}

}
