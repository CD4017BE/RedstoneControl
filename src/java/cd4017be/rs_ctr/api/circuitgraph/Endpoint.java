package cd4017be.rs_ctr.api.circuitgraph;

/**
 * General signal graph entry or exit point.
 * @author CD4017BE
 */
public interface Endpoint {
	/**
	 * @return name of this end point (variable name or pin label)
	 */
	String name();
}
