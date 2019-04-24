package cd4017be.rs_ctr.api.circuitgraph;

import javax.annotation.Nullable;

/**
 * Represents an output pin or more generally a signal exit point.
 * @author CD4017BE
 */
public interface Output extends Endpoint {
	/**
	 * @return the Operator that writes the value
	 */
	@Nullable Operator write();
}