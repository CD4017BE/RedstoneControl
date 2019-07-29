package cd4017be.rs_ctr.circuit.gates;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.rs_ctr.circuit.editor.BasicType.ISpecialNodeProvider;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.compile.NodeCompiler;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialCfg;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;
import io.netty.buffer.ByteBuf;


/**
 * Redstone signal input
 * @author CD4017BE
 */
public class Input extends Gate implements ConfigurableGate, ISpecialRender, ISpecialCfg, ISpecialNodeProvider {

	public boolean interrupt = true;
	public int portID;
	public Node getArr;

	public Input(GateType type, int index, int in, int out) {
		super(type, index, in, out);
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
	public void draw(SchematicBoard board, int x, int y) {
		int l = Math.min(label.length(), 5);
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x - 1, y - 1, l, board.parent.zLevel);
	}

	@Override
	public void setupCfgGUI(GuiFrame gui, Runnable updateCfg) {
		new Button(gui, 76, 9, 0, 9, 2, ()-> interrupt ? 1 : 0, (i)-> {
			interrupt = i != 0;
			updateCfg.run();
		}).texture(180, 59).tooltip("gui.rs_ctr.interrupt#");
	}

	@Override
	public void setPosition(int x, int y) {
		super.setPosition(0, y);
	}

	@Override
	public Node createNode(int o, NodeCompiler code) {
		return new Node(code, portID, getArr);
	}

}
