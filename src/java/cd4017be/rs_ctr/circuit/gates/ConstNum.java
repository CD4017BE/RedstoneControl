package cd4017be.rs_ctr.circuit.gates;

import org.objectweb.asm.Type;
import cd4017be.rs_ctr.circuit.editor.BasicType;
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
	protected Object[] compParams() {
		return new Object[] {value};
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
