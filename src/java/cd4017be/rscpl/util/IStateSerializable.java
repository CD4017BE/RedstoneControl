package cd4017be.rscpl.util;


/**
 * 
 * @author CD4017BE
 */
public interface IStateSerializable {

	/**
	 * overrides the internal state with the given one
	 * @param state new state
	 */
	public abstract void setState(StateBuffer state);

	/**
	 * @return the current internal state
	 */
	public abstract StateBuffer getState();

}
