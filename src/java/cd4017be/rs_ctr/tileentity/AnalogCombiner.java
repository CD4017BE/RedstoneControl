package cd4017be.rs_ctr.tileentity;


/**
 * @author CD4017BE
 *
 */
public class AnalogCombiner extends SignalCombiner {

	@Override
	public void process() {
		dirty = false;
		int v = inputs[0], v1;
		if ((v1 = inputs[1]) > v) v = v1;
		if ((v1 = inputs[2]) > v) v = v1;
		if ((v1 = inputs[3]) > v) v = v1;
		output.accept(v);
	}

}
