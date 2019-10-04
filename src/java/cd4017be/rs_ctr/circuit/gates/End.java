package cd4017be.rs_ctr.circuit.gates;

import cd4017be.rs_ctr.circuit.editor.GeneratedGate;
import cd4017be.rs_ctr.circuit.editor.GeneratedType;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.graph.IEndpoint;

/** @author CD4017BE */
public class End extends GeneratedGate implements IEndpoint {

	public End(GeneratedType type, int index) {
		super(type, index);
	}

	@Override
	public Node getEndNode() {
		GeneratedType type = (GeneratedType)this.type;
		return type.getNode(this, type.end);
	}

}
