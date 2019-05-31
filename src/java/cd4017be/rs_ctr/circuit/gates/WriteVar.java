package cd4017be.rs_ctr.circuit.gates;

import static org.objectweb.asm.Opcodes.*;

import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.Pin;
import cd4017be.rscpl.graph.ReadOp;
import cd4017be.rscpl.graph.WriteOp;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;

/**
 * @author CD4017BE
 *
 */
public class WriteVar extends Combinator implements WriteOp, ISpecialRender {

	private boolean isInterrupt;

	/**
	 * @param type
	 * @param index
	 */
	public WriteVar(BasicType type, int index) {
		super(type, index);
	}

	@Override
	public InsnList compile(Context context) {
		Operator op = inputs[inputCount()];
		if (isInterrupt) {
			if (op == null) inputs[inputCount()] = new Read();
			return type.outputs[0].compile(context, inputs, label);
		} else {
			//not actually using the code, just make sure any local variable is freed up
			if (op != null) op.compile(context);
			
			InsnList ins = new InsnList();
			ins.add(new VarInsnNode(ALOAD, Context.THIS_IDX));
			ins.add(inputs[0].compile(context));
			ins.add(new FieldInsnNode(PUTFIELD, context.compiler.C_THIS, label, outType().getDescriptor()));
			return ins;
		}
	}

	@Override
	public boolean hasSideEffects() {
		return true;
	}

	@Override
	public String name() {
		return label;
	}

	@Override
	public int inputCount() {
		return inputs.length - 1;
	}

	@Override
	protected boolean isInputTypeValid(int pin, Type type) {
		return super.isInputTypeValid(pin, type) || pin == inputCount() && type == Type.VOID_TYPE;
	}

	@Override
	public void link(ReadOp read) {
		this.setInput(inputCount(), read);
	}

	private class Read implements Operator {
		@Override
		public Set<Pin> receivers() {return null;}
		@Override
		public Type outType() {return WriteVar.this.outType();}
		@Override
		public int inputCount() {return 0;}
		@Override
		public int getPin() {return 0;}
		@Override
		public Operator getInput(int pin) {return null;}
		@Override
		public Gate<?> getGate() {return null;}
		@Override
		public Operator getActual() {return null;}
		@Override
		public InsnList compile(Context context) {
			InsnList ins = new InsnList();
			ins.add(new VarInsnNode(ALOAD, Context.THIS_IDX));
			ins.add(new FieldInsnNode(GETFIELD, context.compiler.C_THIS, label, outType().getDescriptor()));
			return ins;
		}
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x + 2, y + 2, 5, board.parent.zLevel + 1);
	}

}
