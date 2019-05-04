package cd4017be.rscpl.editor;

import java.util.Iterator;
import java.util.function.IntFunction;

import cd4017be.lib.util.Utils;
import cd4017be.rscpl.editor.InvalidSchematicException.ErrorType;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.Pin;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public abstract class Gate<T extends GateType<T>> {

	public final T type;
	public final int index;
	protected final Operator[] inputs;
	/** -1: checking, 0: unchecked, 1: approved valid */
	public byte check = 0;
	public int rasterX, rasterY;
	public String label = "";
	public final TraceNode[] traces;

	public Gate(T type, int index) {
		this.type = type;
		this.index = index;
		this.inputs = new Operator[type.inputs];
		this.traces = new TraceNode[type.inputs];
	}

	public int inputCount() {
		return inputs.length;
	}

	public Operator getInput(int pin) {
		return inputs[pin];
	}

	public void setInput(int pin, Operator op) {
		inputs[pin] = op;
	}

	protected abstract boolean requiresInput(int pin);

	public abstract int outputCount();

	public abstract Operator getOutput(int pin);

	public void checkValid() throws InvalidSchematicException {
		if (check > 0) return;
		check = -1;
		Operator[] inputs = this.inputs;
		for (int i = 0, l = inputs.length; i < l; i++) {
			Operator pin = inputs[i];
			if (pin != null) {
				Gate<?> node = pin.getGate();
				if (node.check < 0)
					throw new InvalidSchematicException(ErrorType.causalLoop, this, i);
				node.checkValid();
			} else if (requiresInput(i))
				throw new InvalidSchematicException(ErrorType.missingInput, this, i);
		}
		check = 1;
	}

	public void read(ByteBuf data) {
		rasterX = data.readUnsignedByte();
		rasterY = data.readUnsignedByte();
		for (int i = 0; i < traces.length; i++) {
			int n = data.readUnsignedByte();
			if (n == 0) continue;
			TraceNode tn = new TraceNode(null, 0);
			tn.rasterX = data.readUnsignedByte();
			tn.rasterY = data.readUnsignedByte();
			traces[i] = tn;
			while(--n > 0) {
				tn = tn.next = new TraceNode(this, i);
				tn.rasterX = data.readUnsignedByte();
				tn.rasterY = data.readUnsignedByte();
			}
		}
		label = data.readCharSequence(data.readUnsignedByte(), Utils.UTF8).toString();
	}

	public void write(ByteBuf data) {
		data.writeByte(rasterX);
		data.writeByte(rasterY);
		for (int i = 0; i < traces.length; i++) {
			Operator op = inputs[i];
			int p = data.writerIndex(), n = 1;
			data.writeByte(0);
			if (op == null) continue;
			data.writeByte(op.getGate().index);
			data.writeByte(op.getPin());
			for (TraceNode tn = traces[i]; tn != null; tn = tn.next, n++) {
				data.writeByte(tn.rasterX);
				data.writeByte(tn.rasterY);
			}
			data.setByte(p, n);
		}
		int i = data.writeByte(0).writeCharSequence(label, Utils.UTF8);
		data.setByte(data.writerIndex() - i - 1, i);
	}

	public void reconnect(IntFunction<Gate<?>> indexTable) {
		for (int i = 0; i < traces.length; i++) {
			TraceNode tn = traces[i];
			if (tn != null && tn.owner == null) {
				Gate<?> g = indexTable.apply(tn.rasterX);
				setInput(i, g == null ? null : g.getOutput(tn.rasterY));
				g.traces[i] = tn.next;
			}
		}
	}

	public void remove() {
		for (int i = 0; i < inputCount(); i++)
			setInput(i, null);
		for (int i = 0; i < outputCount(); i++) {
			Operator o = getOutput(i);
			for (Iterator<Pin> it = o.receivers().iterator(); it.hasNext();) {
				Pin p = it.next();
				it.remove();//remove the element before setInput does to avoid concurrent modification.
				p.op.setInput(p.idx, null);
			}
		}
	}

	public BoundingBox2D<Gate<?>> getBounds() {
		return new BoundingBox2D<>(this, rasterX, rasterY, type.width, type.height);
	}

	public abstract int getInputHeight(int pin);

	public abstract int getOutputHeight(int pin);

	@SideOnly(Side.CLIENT)
	public abstract TextureAtlasSprite getIcon();

}
