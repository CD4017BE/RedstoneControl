package cd4017be.rscpl.graph;

import cd4017be.rscpl.editor.InvalidSchematicException;

/**
 * Represents the write operator of a variable.
 * @author CD4017BE
 */
public interface IWriteVar extends IVariable {

	void link(IReadVar read) throws InvalidSchematicException;

}
