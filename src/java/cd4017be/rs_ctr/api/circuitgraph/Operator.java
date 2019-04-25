package cd4017be.rs_ctr.api.circuitgraph;

import java.util.Set;

import javax.annotation.Nullable;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

import cd4017be.rs_ctr.processor.Circuit;

/**
 * Represents the operation performed by a gate to compute the result of an individual output pin.<br>
 * So this is basically the implementation side of a gate.
 * @author CD4017BE
 */
public interface Operator {

	/**
	 * compile the gate.
	 * This typically involves calling {@link #compile()} recursively on the input operands.
	 * @param context the method context
	 * @return list of instructions needed to add the gate's result on top of the Java operand stack.
	 */
	InsnList compile(Context context);

	/**
	 * @return number of input pins
	 */
	int inputCount();

	/**
	 * @param pin input pin index
	 * @return the operator providing the input signal
	 */
	@Nullable Operator getInput(int pin);

	/**
	 * sets the given input connection
	 * @param pin input pin index
	 * @param op the operator providing the input signal
	 */
	default void setInput(int pin, @Nullable Operator op) {
		Operator old = getInput(pin);
		if (old == op) return;
		Pin p = new Pin(this, pin);
		if (old != null) old.receivers().remove(p);
		if (op != null) op.receivers().add(p);
	}

	/**
	 * @return set of inputs from other operands that receive this output
	 */
	Set<Pin> receivers();

	/**
	 * @return whether the result is used by more than one receiver
	 */
	default boolean multiUse() {
		return receivers().size() > 1;
	}

	/**
	 * @return the output value type.
	 */
	Type outType();	

	/**
	 * @return whether this operator will access {@link Circuit#outputs}
	 */
	default boolean isOutPin() { return false; }

	/**
	 * @return whether this operator will access {@link Circuit#inputs}
	 */
	default boolean isInPin() { return false; }

	/**
	 * @return whether this operator effects the circuit state
	 */
	default boolean hasSideEffects() { return false; }

	/**
	 * @param pin
	 * @return whether the given input is not always evaluated
	 */
	default boolean isConditional(int pin) { return false; }

}