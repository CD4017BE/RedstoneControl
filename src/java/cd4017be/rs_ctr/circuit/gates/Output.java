package cd4017be.rs_ctr.circuit.gates;

import static cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet.outCode;

import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.graph.IEndpoint;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;

/**
 * @author CD4017BE
 *
 */
public class Output extends Gate implements IEndpoint, ISpecialRender {

	public int portID;
	public Node getArr;

	public Output(GateType type, int index, int in, int out) {
		super(type, index, in, out);
	}

	@Override
	public Node getEndNode() {
		Node in = inputs[0].getNode();
		return new Node(outCode, new Object[] {portID, 2 << portID}, getArr, getArr, in, in);
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		int l = Math.min(label.length(), 5);
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x - l * 4 + 7, y + 5, l, board.parent.zLevel);
	}

	@Override
	public void setPosition(int x, int y) {
		super.setPosition(58, y);
	}

}
