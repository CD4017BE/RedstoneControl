package cd4017be.rs_ctr.api.circuitgraph;

/**
 * Represents a fixed sized Array variable.<br>
 * They are only accessed as reference object and therefore have no write operator.
 * @author CD4017BE
 */
public interface Array extends Variable {
	/**
	 * @return element count
	 */
	int size();

	@Override
	default String desc() {return "[" + Variable.super.desc();}

	@Override
	default Operator write() {return null;}

}