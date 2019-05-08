package cd4017be.rs_ctr.circuit.gates;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.SIPUSH;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;
import io.netty.buffer.ByteBuf;


/**
 * @author CD4017BE
 *
 */
public class ConstNum extends Combinator implements ConfigurableGate, ISpecialRender {

	public Number value;

	/**
	 * @param type
	 * @param index
	 */
	public ConstNum(BasicType type, int index) {
		super(type, index);
		this.value = 0;
	}

	@Override
	public InsnList compile(Context context) {
		InsnList ins = new InsnList();
		if (outType() == Type.INT_TYPE) {
			int val = (Integer)value;
			if (val >= -1 && val <= 5)
				ins.add(new InsnNode(ICONST_0 + val));
			else if (val >= Byte.MIN_VALUE && val < Byte.MAX_VALUE)
				ins.add(new IntInsnNode(BIPUSH, val));
			else if (val >= Short.MIN_VALUE && val < Short.MAX_VALUE)
				ins.add(new IntInsnNode(SIPUSH, val));
			else ins.add(new LdcInsnNode(val));
		} else ins.add(new LdcInsnNode(value));
		return ins;
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
	public void draw(SchematicBoard board, int x, int y) {
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x + 2, y + 2, 5, board.parent.zLevel + 1);
	}

}
