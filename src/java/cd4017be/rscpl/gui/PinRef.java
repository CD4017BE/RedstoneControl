package cd4017be.rscpl.gui;

import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.graph.Operator;

/**
 * 
 * @author CD4017BE
 *
 */
public class PinRef {

	public final int gate, pin, trace;
	public final int x, y;
	public final PinRef link;

	public PinRef(Operator out) {
		Gate<?> g = out.getGate();
		this.gate = g.index;
		this.pin = out.getPin();
		this.trace = -1;
		this.x = g.rasterX + g.type.width;
		this.y = g.rasterY + g.getOutputHeight(pin);
		this.link = null;
	}

	public PinRef(Gate<?> gate, int pin) {
		this.gate = gate.index;
		this.pin = pin;
		this.trace = 0;
		this.x = gate.rasterX;
		this.y = gate.rasterY + gate.getInputHeight(pin);
		Operator out = gate.getInput(pin);
		PinRef src = out != null ? new PinRef(out) : null;
		this.link = src;
		//TODO traces
	}

	@Override
	public int hashCode() {
		return x & 0xffff | y << 16;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PinRef)) return false;
		PinRef p = (PinRef)o;
		return gate == p.gate && pin == p.pin && trace == p.trace;
	}

}
