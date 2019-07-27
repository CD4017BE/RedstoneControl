package cd4017be.rscpl.editor;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;

import cd4017be.rscpl.compile.Node;

/**
 * 
 * @author CD4017BE
 */
public class Pin {

	public final Gate gate;
	public final int idx;
	public final Set<Pair<Gate, Integer>> receivers;
	Node node;

	public Pin(Gate gate, int pin) {
		this.gate = gate;
		this.idx = pin;
		this.receivers = new HashSet<>();
	}

	public Node getNode() {
		if (node != null) return node;
		return node = gate.type.createNode(gate, idx);
	}

	public Type getOutType() {
		return gate.type.getOutType(idx);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + idx;
		result = prime * result + gate.index;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Pin)) return false;
		Pin other = (Pin)obj;
		return idx == other.idx && gate == other.gate;
	}

	@Override
	public String toString() {
		return gate.toString() + '#' + Integer.toString(idx);
	}

}