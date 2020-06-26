package cd4017be.rscpl.graph;

import org.objectweb.asm.Type;

import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.util.StateBuffer;
import static org.objectweb.asm.Type.*;

/**
 * Represents the read operator of a variable.
 * @author CD4017BE
 */
public interface IReadVar extends IVariable, ValueOp {

	default void initState(StateBuffer state) {
		String name = name();
		Type t = type();
		switch(t.getSort()) {
		case BYTE: state.set(name, ((Number)getValue()).byteValue()); break;
		case SHORT: state.set(name, ((Number)getValue()).shortValue()); break;
		case INT: state.set(name, ((Number)getValue()).intValue()); break;
		case LONG: state.set(name, ((Number)getValue()).longValue()); break;
		case FLOAT: state.set(name, ((Number)getValue()).floatValue()); break;
		case DOUBLE: state.set(name, ((Number)getValue()).doubleValue()); break;
		case ARRAY:
			switch(t.getElementType().getSort()) {
			case BYTE: state.set(name, (byte[])getValue()); break;
			case SHORT: state.set(name, (short[])getValue()); break;
			case INT: state.set(name, (int[])getValue()); break;
			case FLOAT: state.set(name, (float[])getValue()); break;
			} break;
		}
	}

	Node result();

}
