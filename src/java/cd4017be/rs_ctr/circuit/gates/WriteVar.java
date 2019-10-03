package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Type;

import cd4017be.rs_ctr.circuit.editor.GeneratedGate;
import cd4017be.rs_ctr.circuit.editor.GeneratedType;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.graph.IEndpoint;
import cd4017be.rscpl.graph.IReadVar;
import cd4017be.rscpl.graph.IWriteVar;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;

/**
 * @author CD4017BE
 *
 */
public class WriteVar extends GeneratedGate implements IWriteVar, ISpecialRender, IEndpoint {

	IReadVar link;

	public WriteVar(GeneratedType type, int index) {
		super(type, index);
	}

	@Override
	public String name() {
		return label;
	}

	@Override
	public void link(IReadVar read) {
		link = read;
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x + 2, y + 2, 5, board.parent.zLevel + 1);
	}

	@Override
	public Type type() {
		return type.getInType(0);
	}

	@Override
	protected Node createLink(int i) {
		return link.result();
	}

}
