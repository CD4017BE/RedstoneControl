package cd4017be.rs_ctr.circuit.gates;

import cd4017be.rs_ctr.circuit.editor.GeneratedGate;
import cd4017be.rs_ctr.circuit.editor.GeneratedType;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;

/** Redstone signal input
 * @author CD4017BE */
public class Input extends GeneratedGate implements ISpecialRender {

	public int portID;

	public Input(GeneratedType type, int index) {
		super(type, index);
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		int l = Math.min(label.length(), 5);
		GateTextureHandler.drawTinyText(
			board.parent.getDraw(), label, x - 1, y - 1, l, board.parent.zLevel
		);
	}

	@Override
	public void setPosition(int x, int y) {
		super.setPosition(0, y);
	}

	@Override
	public Object getParam(int i) {
		return i < 0 ? portID : super.getParam(i);
	}

}
