package cd4017be.rscpl.editor;

/**
 * @author CD4017BE
 *
 */
public abstract class GateType<T extends GateType<T>> {

	public final String name;
	public final int inputs;
	public final int width, height;

	public GateType(String name, int width, int height, int inputs) {
		this.name = name;
		this.width = width;
		this.height = height;
		this.inputs = inputs;
	}

	public abstract Gate<T> newGate(int index);

	@FunctionalInterface
	public static interface GateConstructor<T extends GateType<T>> {
		Gate<T> newGate(T type, int index);
	}

	public String getIcon() {
		return name;
	}

}
