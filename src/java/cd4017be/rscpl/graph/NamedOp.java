package cd4017be.rscpl.graph;


/**
 * @author CD4017BE
 *
 */
public interface NamedOp extends Operator {
	/**
	 * @return name of this end point (variable name or pin label)
	 */
	String name();
}
