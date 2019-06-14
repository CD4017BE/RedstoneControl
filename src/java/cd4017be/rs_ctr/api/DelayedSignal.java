package cd4017be.rs_ctr.api;


/**
 * Holds a gate's input signal to be processed later. Used to ensure signal tick synchronization.
 * @author CD4017BE
 */
public class DelayedSignal {

	/**the port this signal should be send to */
	public final int id;
	/**the value to send*/
	public int value;
	/**the next element so this can be used as linked list */
	public DelayedSignal next;

	public DelayedSignal(int id, int value) {
		this(id, value, null);
	}

	public DelayedSignal(int id, int value, DelayedSignal next) {
		this.id = id;
		this.value = value;
		this.next = next;
	}

}
