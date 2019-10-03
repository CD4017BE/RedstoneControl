package cd4017be.rscpl.editor;

import org.objectweb.asm.Type;

import cd4017be.rscpl.compile.Node;

/**
 * @author CD4017BE
 *
 */
public abstract class GateType {

	public final String name;
	public final int width, height;

	public GateType(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
	}

	public abstract Gate newGate(int index);

	public abstract int getInputHeight(int i);

	public abstract int getOutputHeight(int o);

	public abstract Type getOutType(int o);

	public abstract Type getInType(int o);

	public abstract boolean isInputTypeValid(int i, Type type);

	public abstract Node createNode(Gate gate, int o);

	@FunctionalInterface
	public static interface GateConstructor {
		Gate newGate(GateType type, int index, int in, int out);
	}

	public String getIcon() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
