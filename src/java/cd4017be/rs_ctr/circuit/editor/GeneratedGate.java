package cd4017be.rs_ctr.circuit.editor;

import java.util.Arrays;

import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;

/**
 * 
 * @author cd4017be
 *
 */
public class GeneratedGate extends Gate {

	protected final Node[] nodeCache;

	public GeneratedGate(GeneratedType type, int index) {
		super(type, index, type.inputs, type.outputs.length);
		this.nodeCache = new Node[type.nodes.length + type.links];
	}

	@Override
	public void checkValid() throws InvalidSchematicException {
		Arrays.fill(nodeCache, null);
		super.checkValid();
	}

	public void setLink(Node n, int i) {
		if (i < ((GeneratedType)type).links)
			nodeCache[i] = n;
	}

	protected Node createLink(int i) {
		throw new UnsupportedOperationException();
	}

	public Node getEndNode() {
		GeneratedType type = (GeneratedType)this.type;
		return type.getNode(this, type.end);
	}

	public interface IParameterizedGate {
		Object getParam(int i);
	}

}
