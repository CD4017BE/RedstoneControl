package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Type;

import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;

/**
 * @author CD4017BE
 *
 */
public class Output extends Combinator implements ISpecialRender {

	public int portID;

	/**
	 * @param type
	 * @param index
	 */
	public Output(BasicType type, int index) {
		super(type, index);
	}

	@Override
	public boolean hasSideEffects() {
		return true;
	}

	@Override
	public int outputCount() {
		return 0; //invisible output pin
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		int l = Math.min(label.length(), 5);
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x - l * 4 + 7, y + 5, l, board.parent.zLevel);
	}

	@Override
	protected Object[] compParams() {
		return new Object[] {portID, 2 << portID};
	}
	@Override
	protected boolean isInputTypeValid(int pin, Type type) {
		if (pin == 0) return type == Type.INT_TYPE;
		return true;
	}
}
