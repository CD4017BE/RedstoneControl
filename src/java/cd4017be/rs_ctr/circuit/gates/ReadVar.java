package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;

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
	public InsnList compile(Context context) {
		InsnList ins = new InsnList();
		ins.add(new FieldInsnNode(Opcodes.GETFIELD, context.compiler.C_THIS, label, outType().getDescriptor()));
		return ins;
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
