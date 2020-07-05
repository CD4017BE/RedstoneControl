package cd4017be.rs_ctr.tileentity;

import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rs_ctr.gui.GuiProcessor;

/**TileEntity interface used for {@link GuiProcessor}
 * @author CD4017BE */
public interface IProcessor {

	String getName();
	String getError();
	double getExhaustion();
	String[] getIOLabels();
	Circuit getCircuit();
	int getClockState();

}
