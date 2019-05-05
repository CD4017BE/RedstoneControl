package cd4017be.rs_ctr.circuit.gates;

import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.editor.ConfigurableGate;
import io.netty.buffer.ByteBuf;


/**
 * Redstone signal input
 * @author CD4017BE
 */
public class Input extends Combinator implements ConfigurableGate {

	public boolean interrupt = true;

	/**
	 * @param type
	 * @param index
	 */
	public Input(BasicType type, int index) {
		super(type, index);
	}

	@Override
	public void writeCfg(ByteBuf data) {
		data.writeBoolean(interrupt);
	}

	@Override
	public void readCfg(ByteBuf data) {
		interrupt = data.readBoolean();
	}

	@Override
	public boolean isInPin() {
		return true;
	}

}
