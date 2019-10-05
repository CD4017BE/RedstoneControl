package cd4017be.rscpl.editor;

import java.util.function.IntFunction;

/**
 * @author CD4017BE
 *
 */
public class InvalidSchematicException extends Exception {
	private static final long serialVersionUID = 1L;

	public final int errcode;
	public final Gate gate;
	public final int pin;

	public InvalidSchematicException(int errcode, Gate node, int pin) {
		this.errcode = errcode;
		this.gate = node;
		this.pin = pin;
	}

	public InvalidSchematicException(int compact, IntFunction<Gate> gateGetter) {
		this.errcode = compact & 0xff;
		if (errcode == NO_ERROR) gate = null;
		else gate = gateGetter.apply(compact >> 8 & 0xffff);
		this.pin = compact >> 24;
	}

	public int compact() {
		return errcode & 0xff
			| (gate == null ? -1 : gate.index << 8) & 0xffff00
			| pin << 24 & 0xff000000;
	}

	public static final int 
		NO_ERROR = 0,
		CAUSAL_LOOP = 1,
		TYPE_MISSMATCH = 2,
		MISSING_INPUT = 3,
		INVALID_LABEL = 4,
		READ_CONFLICT = 5,
		VAR_TYPE_CONFLICT = 6,
		INVALID_CFG = 7;

}
