package cd4017be.rscpl.editor;

public class InstructionSet {

	private final GateType<?>[] REGISTRY = new GateType[256];

	public void add(int id0, GateType<?>... types) {
		for (GateType<?> t : types) {
			REGISTRY[id0] = t;
			t.id = id0++;
		}
	}

	public GateType<?> get(int id) {
		return REGISTRY[id & 0xff];
	}

	public Gate<?> newGate(int id, int index) {
		GateType<?> t = REGISTRY[id & 0xff];
		return t != null ? t.newGate(index) : null;
	}

}