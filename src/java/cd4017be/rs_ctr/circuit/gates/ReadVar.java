package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.graph.ReadOp;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialCfg;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;
import io.netty.buffer.ByteBuf;


/**
 * @author CD4017BE
 *
 */
public class ReadVar extends Combinator implements ReadOp, ConfigurableGate, ISpecialRender, ISpecialCfg {

	public Number value;

	/**
	 * @param type
	 * @param index
	 */
	public ReadVar(BasicType type, int index) {
		super(type, index);
		this.value = 0;
	}

	@Override
	public void compile(MethodVisitor mv, Context context) {
		if (mv == null) return;
		mv.visitVarInsn(Opcodes.ALOAD, Context.THIS_IDX);
		mv.visitFieldInsn(Opcodes.GETFIELD, context.compiler.C_THIS, label, outType().getDescriptor());
	}

	@Override
	public void writeCfg(ByteBuf data) {
		switch(outType().getSort()) {
		default:
		case Type.INT: data.writeInt(value.intValue()); break;
		case Type.LONG: data.writeLong(value.longValue()); break;
		case Type.FLOAT: data.writeFloat(value.floatValue()); break;
		case Type.DOUBLE: data.writeDouble(value.doubleValue()); break;
		}
	}

	@Override
	public void readCfg(ByteBuf data) {
		switch(outType().getSort()) {
		default:
		case Type.INT: value = data.readInt(); break;
		case Type.LONG: value = data.readLong(); break;
		case Type.FLOAT: value = data.readFloat(); break;
		case Type.DOUBLE: value = data.readDouble(); break;
		}
	}

	@Override
	public String name() {
		return label;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x + 2, y + 2, 5, board.parent.zLevel + 1);
	}

	@Override
	public void setupCfgGUI(GuiFrame gui, Runnable updateCfg) {
		gui.bgY = 32;
		new TextField(gui, 74, 7, 1, 10, 20, ()-> value.toString(), (s)-> {
			try {
				value = ConstNum.parse(s, outType());
				updateCfg.run();
			} catch (NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.value");
	}

}
