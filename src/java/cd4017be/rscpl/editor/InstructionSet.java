package cd4017be.rscpl.editor;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author CD4017BE
 */
public class InstructionSet {

	protected final Map<String, Integer> IDS = new HashMap<>();
	protected final GateType<?>[] REGISTRY = new GateType[256];

	public void add(int id0, GateType<?>... types) {
		for (GateType<?> t : types) {
			REGISTRY[id0] = t;
			IDS.put(t.name, id0++);
		}
	}

	public GateType<?> get(int id) {
		return REGISTRY[id & 0xff];
	}

	public int id(GateType<?> type) {
		return IDS.get(type.name);
	}

	public Gate<?> newGate(int id, int index) {
		GateType<?> t = REGISTRY[id & 0xff];
		return t != null ? t.newGate(index) : null;
	}

}