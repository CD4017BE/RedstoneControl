package cd4017be.rscpl.editor;

/**
 * @author CD4017BE
 *
 */
public class TraceNode {

	public final Gate owner;
	public final int pin;
	public int rasterX, rasterY;
	public TraceNode next;

	public TraceNode(Gate owner, int pin) {
		this.owner = owner;
		this.pin = pin;
	}

	public TraceNode copy(Gate owner, int pin) {
		TraceNode n = new TraceNode(owner, pin);
		n.rasterX = rasterX;
		n.rasterY = rasterY;
		if (next != null)
			n.next = next.copy(owner, pin);
		return n;
	}

}
