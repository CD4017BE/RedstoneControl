package cd4017be.rs_ctr.api.circuitgraph;

/**
 * Represents an input pin or more generally a signal entry point.
 * @author CD4017BE
 */
public interface Input extends Endpoint {
	/**
	 * @return whether the circuit should update when its value changes
	 */
	boolean isInterrupt();
	/**
	 * @return the Operator that reads the value
	 */
	Operator read();
}