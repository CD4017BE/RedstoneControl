package cd4017be.rscpl.gui;

import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.Pin;
import cd4017be.rscpl.editor.TraceNode;

/**
 * 
 * @author CD4017BE
 *
 */
public class PinRef {

	public final int gate, pin, trace;
	public final int x, y;
	public final PinRef link;

	public PinRef(Pin out) {
		Gate g = out.gate;
		this.gate = g.index;
		this.pin = out.idx;
		this.trace = -1;
		this.x = g.rasterX + Math.max(0, g.type.width);
		this.y = g.rasterY + g.type.getOutputHeight(pin);
		this.link = null;
	}

	public PinRef(Gate gate, int pin) {
		this.gate = gate.index;
		this.pin = pin;
		this.trace = 0;
		this.x = gate.rasterX + Math.min(0, gate.type.width);
		this.y = gate.rasterY + gate.type.getInputHeight(pin);
		TraceNode tn = gate.traces[pin];
		if (tn == null) {
			Pin out = gate.getInput(pin);
			this.link = out != null ? new PinRef(out) : null;
		} else this.link = new PinRef(tn, 1);
	}

	public PinRef(TraceNode tn, int depth) {
		this.gate = tn.owner.index;
		this.pin = tn.pin;
		this.trace = depth;
		this.x = tn.rasterX;
		this.y = tn.rasterY;
		if (tn.next == null) {
			Pin out = tn.owner.getInput(pin);
			this.link = out != null ? new PinRef(out) : null;
		} else this.link = new PinRef(tn.next, depth + 1);
	}

	public PinRef(PinRef ref, int x, int y) {
		this.gate = ref.gate;
		this.pin = ref.pin;
		this.trace = ref.trace + 1;
		this.x = x;
		this.y = y;
		this.link = ref.link;
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
