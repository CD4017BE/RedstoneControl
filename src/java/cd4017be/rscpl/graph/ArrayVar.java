package cd4017be.rscpl.graph;

import java.lang.reflect.Array;

/**
 * Represents a fixed sized Array variable.
 * @author CD4017BE
 */
public interface ArrayVar extends ReadOp {

	/**
	 * @return element count
	 */
	default int size() {
		return Array.getLength(getValue());
	}

	@Override
	default int memoryUsage() {
		return NamedOp.memoryUsage(outType().getElementType()) * size();
	}

}
