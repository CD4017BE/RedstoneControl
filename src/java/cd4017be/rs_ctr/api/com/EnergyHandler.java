package cd4017be.rs_ctr.api.com;


/**
 * Callback interface for transmitting Energy.
 * @author CD4017BE
 */
@FunctionalInterface
public interface EnergyHandler {

	/**
	 * called to transmit energy to/from this device
	 * @param dE positive value to supply, negative value to request energy<br>(unit equivalent to Forge Energy / Redstone Flux)
	 * @param sim whether the transmission should only be simulated
	 * @return amount of energy actually/theoretically transfered (value must have the same sign as <b>dE</b> and an equal or smaller magnitude)
	 */
	int changeEnergy(int dE, boolean sim);

	/**implementation that does nothing, useful to remove the need for null checks. */
	public static final EnergyHandler NOP = (dE, sim)-> 0;

}
