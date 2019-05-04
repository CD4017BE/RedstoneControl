package cd4017be.rscpl.editor;


/**
 * @author CD4017BE
 *
 */
public abstract class GateType<T extends GateType<T>> {

	public final int inputs;
	public final int width, height;
	int id;

	public GateType(int width, int height, int inputs) {
		this.width = width;
		this.height = height;
		this.inputs = inputs;
	}

	public abstract Gate<T> newGate(int index);

	public int id() {
		return id;
	}

	@FunctionalInterface
	public static interface GateConstructor<T extends GateType<T>> {
		Gate<T> newGate(T type, int index);
	}

}
