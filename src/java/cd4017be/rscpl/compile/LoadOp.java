package cd4017be.rscpl.compile;

import java.util.Collections;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

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
	public InsnList compile(Context context) {
		InsnList ins = new InsnList();
		Type t = source.outType();
		Branch b = (Branch)source;
		ins.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), b.localIdx));
		if (--b.uses == 0)
			context.releaseLocal(b.localIdx, t);
		return ins;
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

}
