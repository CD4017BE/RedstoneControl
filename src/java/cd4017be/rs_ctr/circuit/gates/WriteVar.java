package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Type;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rs_ctr.circuit.editor.BasicType.ISpecialNodeProvider;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.compile.NodeCompiler;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
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
public class WriteVar extends Gate implements IWriteVar, ISpecialRender, ISpecialCfg, ConfigurableGate, IEndpoint, ISpecialNodeProvider {

	private boolean interrupt = true;
	IReadVar link;

	public WriteVar(GateType type, int index, int in, int out) {
		super(type, index, in, type.getOutType(0) != Type.VOID_TYPE ? 1 : 0);
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
		return ((BasicType)type).outputs[0].getInType(0);
	}

	@Override
	public Node getEndNode() {
		if (outputs.length > 0) return outputs[0].getNode();
		return createNode(0, null);
	}

	@Override
	public Node createNode(int o, NodeCompiler code) {
		Node n = inputs[0].getNode();
		if (interrupt) {
			Node ref = link != null ?
					((Gate)link).outputs[0].getNode() :
					new Node(new ReadVar.Compiler(type()), label);
			return new Node(((BasicType)type).outputs[1], label, n, ref, n);
		}
		return new Node(((BasicType)type).outputs[0], label, n);
	}

}
