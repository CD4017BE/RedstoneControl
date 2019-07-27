package cd4017be.rscpl.graph;


/**
 * Represents the write operator of a variable.
 * @author CD4017BE
 */
public interface IWriteVar extends IVariable {

	void link(IReadVar read);

}
