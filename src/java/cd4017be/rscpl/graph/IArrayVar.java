package cd4017be.rscpl.graph;

import java.lang.reflect.Array;

/**
 * Represents a fixed sized Array variable.
 * @author CD4017BE
 */
public interface IArrayVar extends IReadVar {

	/**
	 * @return element count
	 */
	default int size() {
		return Array.getLength(getValue());
	}

	@Override
	default int memoryUsage() {
		return IVariable.memoryUsage(type().getElementType()) * size();
	}

}
