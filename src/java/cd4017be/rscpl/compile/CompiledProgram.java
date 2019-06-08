package cd4017be.rscpl.compile;

import cd4017be.rscpl.util.StateBuffer;

/**
 * @author CD4017BE
 *
 */
public interface CompiledProgram {

	void setCode(byte[] code);
	StateBuffer getState();

}
