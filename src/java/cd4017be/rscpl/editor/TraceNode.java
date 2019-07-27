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

}
