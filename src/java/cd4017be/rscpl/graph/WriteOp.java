package cd4017be.rscpl.graph;


/**
 * Represents the write operator of a variable.
 * @author CD4017BE
 */
public interface WriteOp extends NamedOp {

	void link(ReadOp read);

}
