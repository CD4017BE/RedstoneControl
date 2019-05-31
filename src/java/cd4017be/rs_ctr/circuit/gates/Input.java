package cd4017be.rs_ctr.circuit.gates;

import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;
import io.netty.buffer.ByteBuf;


/**
 * Redstone signal input
 * @author CD4017BE
 */
public class Input extends Combinator implements ConfigurableGate, ISpecialRender {

	public boolean interrupt = true;
	public int portID;

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

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		int l = Math.min(label.length(), 5);
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x - 1, y - 1, l, board.parent.zLevel);
	}

}
