package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.graph.ReadOp;


/**
 * @author CD4017BE
 *
 */
public class ReadVar extends ConstNum implements ReadOp {

	/**
	 * @param type
	 * @param index
	 */
	public ReadVar(BasicType type, int index) {
		super(type, index);
	}

	@Override
	public void compile(MethodVisitor mv, Context context) {
		if (mv == null) return;
		mv.visitVarInsn(Opcodes.ALOAD, Context.THIS_IDX);
		mv.visitFieldInsn(Opcodes.GETFIELD, context.compiler.C_THIS, label, outType().getDescriptor());
	}

	@Override
	public String name() {
		return label;
	}

	@Override
	public Object getValue() {
		return value;
	}

}
