package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.rs_ctr.circuit.editor.BasicType.ISpecialNodeProvider;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.compile.Dep;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.compile.NodeCompiler;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.graph.IReadVar;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialCfg;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;
import io.netty.buffer.ByteBuf;


/**
 * @author CD4017BE
 *
 */
public class ReadVar extends Gate implements IReadVar, ConfigurableGate, ISpecialRender, ISpecialCfg, ISpecialNodeProvider {

	public Number value;

	public ReadVar(GateType type, int index, int in, int out) {
		super(type, index, in, out);
		this.value = 0;
	}

	@Override
	public void writeCfg(ByteBuf data) {
		switch(type().getSort()) {
		default:
		case Type.INT: data.writeInt(value.intValue()); break;
		case Type.LONG: data.writeLong(value.longValue()); break;
		case Type.FLOAT: data.writeFloat(value.floatValue()); break;
		case Type.DOUBLE: data.writeDouble(value.doubleValue()); break;
		}
	}

	@Override
	public void readCfg(ByteBuf data) {
		switch(type().getSort()) {
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
				value = ConstNum.parse(s, type());
				updateCfg.run();
			} catch (NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.value");
	}

	@Override
	public Type type() {
		return type.getOutType(0);
	}

	@Override
	public Node createNode(int o, NodeCompiler code) {
		return new Node(code, label);
	}

	public static class Compiler implements NodeCompiler {

		public final Type varType;

		public Compiler(Type varType) {
			this.varType = varType;
		}

		@Override
		public Type getInType(int i) {
			return null;
		}

		@Override
		public Type getOutType() {
			return varType;
		}

		@Override
		public void compile(Dep[] inputs, Object param, MethodVisitor mv, Context context) {
			mv.visitVarInsn(Opcodes.ALOAD, Context.THIS_IDX);
			mv.visitFieldInsn(Opcodes.GETFIELD, context.compiler.C_THIS, (String)param, varType.getDescriptor());
		}

	}

}
