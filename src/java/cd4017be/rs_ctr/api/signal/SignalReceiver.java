package cd4017be.rs_ctr.api.signal;

import java.util.function.IntConsumer;

/**
 * Simple port callback implementation that just stores the received signal, useful for polling based device implementations.
 * @author CD4017BE
 */
public class SignalReceiver implements IntConsumer {

	public int state;

	public SignalReceiver(int state) {
		this.state = state;
	}

	@Override
	public void accept(int val) {
		state = val;
	}

	/**Port callback implementation that does nothing, useful to remove the need for null checks. */
	public static final IntConsumer NOP = (v)->{};

}
