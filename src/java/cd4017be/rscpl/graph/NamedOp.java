package cd4017be.rscpl.graph;

import org.objectweb.asm.Type;

/**
 * @author CD4017BE
 *
 */
public interface NamedOp extends Operator {
	/**
	 * @return name of this end point (variable name or pin label)
	 */
	String name();

	/**
	 * @return number of bytes used for this variable
	 */
	default int memoryUsage() {
		return memoryUsage(outType());
	}

	public static int memoryUsage(Type type) {
		switch(type.getSort()) {
		case Type.BOOLEAN:
		case Type.BYTE: return 1;
		case Type.CHAR:
		case Type.SHORT: return 2;
		case Type.FLOAT:
		case Type.INT: return 4;
		case Type.DOUBLE:
		case Type.LONG: return 8;
		default: return 4;
		}
	}

}
