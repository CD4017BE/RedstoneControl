package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Type;

import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.IGuiComp;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.rs_ctr.circuit.editor.GeneratedGate;
import cd4017be.rs_ctr.circuit.editor.GeneratedType;
import cd4017be.rscpl.editor.InvalidSchematicException;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialCfg;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;


/**
 * @author CD4017BE
 *
 */
public class ConstNum extends GeneratedGate implements ISpecialRender, ISpecialCfg {

	public Number value;

	/**
	 * @param type
	 * @param index
	 */
	public ConstNum(GeneratedType type, int index) {
		super(type, index);
	}

	@Override
	public void checkValid() throws InvalidSchematicException {
		super.checkValid();
		try {
			this.value = parse(label, type.getOutType(0));
		} catch (NumberFormatException e) {
			throw new InvalidSchematicException(InvalidSchematicException.INVALID_CFG, this, 0);
		}
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x + 2, y + 2, 5, board.parent.zLevel + 1);
	}

	public static Number parse(String s, Type t) throws NumberFormatException {
		if (s.isEmpty()) throw new NumberFormatException();
		int rad;
		if (s.charAt(0) == 'x') {s = s.substring(1); rad = 16;}
		else if (s.charAt(0) == 'b') {s = s.substring(1); rad = 2;}
		else rad = 10;
		switch(t.getSort()) {
		case Type.INT: return Integer.valueOf(s, rad);
		case Type.LONG: return Long.valueOf(s, rad);
		case Type.FLOAT: return Float.valueOf(s);
		case Type.DOUBLE: return Double.valueOf(s);
		default: throw new NumberFormatException();
		}
	}

	@Override
	public void setupCfgGUI(GuiFrame gui, Runnable updateCfg) {
		for (IGuiComp c : gui)
			if (c instanceof TextField)
				((TextField)c).tooltip("gui.rs_ctr.value");
	}

	@Override
	public Object getParam(int i) {
		return value;
	}

}
