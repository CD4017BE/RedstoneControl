package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Type;
import cd4017be.rs_ctr.circuit.editor.GeneratedGate;
import cd4017be.rs_ctr.circuit.editor.GeneratedType;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.graph.IEndpoint;
import cd4017be.rscpl.graph.IReadVar;
import cd4017be.rscpl.graph.IWriteVar;

/** @author CD4017BE */
public class ReadWriteVar extends GeneratedGate
implements IReadVar, IWriteVar, IEndpoint {

	public ReadWriteVar(GeneratedType type, int index) {
		super(type, index);
	}

	@Override
	public String name() {
		return label;
	}

	@Override
	public Type type() {
		return type.getOutType(0);
	}

	@Override
	public Object getValue() {
		return getParam(0);
	}

	@Override
	public void link(IReadVar read) {
	}

	@Override
	public Node result() {
		return null;
	}

	@Override
	protected boolean isIndependent(int out) {
		return true;
	}

}
