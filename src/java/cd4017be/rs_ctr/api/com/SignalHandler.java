package cd4017be.rs_ctr.api.com;

/**
 * Callback interface for transmitting Redstone signals.
 * @author CD4017BE
 */
@FunctionalInterface
public interface SignalHandler {

	/**
	 * called whenever the transmitted signal value changes
	 * @param value the new signal value
	 */
	void updateSignal(int value);

	/**implementation that does nothing, useful to remove the need for null checks. */
	public static final SignalHandler NOP = (v)->{};

}
