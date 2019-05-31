package cd4017be.rscpl.compile;

import java.util.Collections;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.Pin;


/**
 * Internal operator used to handle loading of intermediate results stored in local variables.
 * @author CD4017BE
 */
public class LoadOp extends Pin implements Operator {

	private Operator source;

	/**
	 * @param owner
	 * @param pin
	 */
	public LoadOp(Operator receiver, int pin, Operator source) {
		super(receiver, pin);
		receiver.setInput(pin, this);
		this.setInput(0, source);
	}

	@Override
	public void compile(MethodVisitor mv, Context context) {
		Type t = source.outType();
		Branch b = (Branch)source;
		if (mv != null) mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), b.localIdx);
		if (--b.uses == 0)
			context.releaseLocal(b.localIdx, t);
	}

	@Override
	public int inputCount() {
		return 1;
	}

	@Override
	public Operator getInput(int pin) {
		return source;
	}

	@Override
	public void setInput(int pin, Operator op) {
		Operator.super.setInput(pin, op);
		this.source = op;
	}

	@Override
	public Set<Pin> receivers() {
		return Collections.singleton(this);
	}

	@Override
	public Type outType() {
		return source.outType();
	}

	@Override
	public Gate<?> getGate() { return null; }

	@Override
	public int getPin() { return 0; }

	@Override
	public Operator getActual() {
		return source == null ? null : source.getActual();
	}

}
