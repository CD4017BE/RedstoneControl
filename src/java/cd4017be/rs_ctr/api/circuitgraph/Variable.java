package cd4017be.rs_ctr.api.circuitgraph;

import static org.objectweb.asm.Opcodes.T_BYTE;
import static org.objectweb.asm.Opcodes.T_DOUBLE;
import static org.objectweb.asm.Opcodes.T_FLOAT;
import static org.objectweb.asm.Opcodes.T_INT;
import static org.objectweb.asm.Opcodes.T_LONG;
import static org.objectweb.asm.Opcodes.T_SHORT;

/**
 * Represents a variable.
 * @author CD4017BE
 */
public interface Variable extends Input, Output {
	/**
	 * @return type of this variable
	 */
	int type();
	/**
	 * @return Java descriptor of this variable
	 */
	default String desc() {
		switch(type()) {
		case T_BYTE: return "B";
		case T_SHORT: return "S";
		case T_INT: return "I";
		case T_FLOAT: return "F";
		case T_LONG: return "J";
		case T_DOUBLE: return "D";
		default: return null;
		}
	}

	@Override //typically handled by the write operator instead
	default boolean isInterrupt() {return false;}

}