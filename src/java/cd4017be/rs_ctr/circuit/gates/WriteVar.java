package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Type;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.rs_ctr.circuit.editor.GeneratedGate;
import cd4017be.rs_ctr.circuit.editor.GeneratedGate.IParameterizedGate;
import cd4017be.rs_ctr.circuit.editor.GeneratedType;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.graph.IEndpoint;
import cd4017be.rscpl.graph.IReadVar;
import cd4017be.rscpl.graph.IWriteVar;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialCfg;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;
import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public class WriteVar extends GeneratedGate implements IWriteVar, ISpecialRender, ISpecialCfg, ConfigurableGate, IEndpoint, IParameterizedGate {

	private boolean interrupt = true;
	IReadVar link;

	public WriteVar(GeneratedType type, int index) {
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
	public void setupCfgGUI(GuiFrame gui, Runnable updateCfg) {
		new Button(gui, 76, 9, 0, 9, 2, ()-> interrupt ? 1 : 0, (i)-> {
			interrupt = i != 0;
			updateCfg.run();
		}).texture(180, 59).tooltip("gui.rs_ctr.interrupt#");
	}

	@Override
	public Type type() {
		return Type.INT_TYPE;
	}

	@Override
	protected Node createLink(int i) {
		return link.result();
	}

	/*@Override
	public Node createNode(int o, NodeCompiler code) {
		Node n = inputs[0].getNode();
		if (interrupt) {
			Node ref = link != null ?
					((Gate)link).outputs[0].getNode() :
					new Node(new ReadVar.Compiler(type()), label);
			return new Node(((BasicType)type).outputs[1], label, n, ref, n);
		}
		return new Node(((BasicType)type).outputs[0], label, n);
	}*/

	@Override
	public Object getParam(int i) {
		return interrupt;
	}

}
