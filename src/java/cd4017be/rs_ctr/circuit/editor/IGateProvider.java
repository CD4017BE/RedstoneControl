package cd4017be.rs_ctr.circuit.editor;

import java.util.HashMap;

@FunctionalInterface
public interface IGateProvider {

	GeneratedGate newGate(GeneratedType type, int index);

	public static final HashMap<String, IGateProvider> REGISTRY = new HashMap<>();

	public static final IGateProvider DEFAULT = GeneratedGate::new;
}
