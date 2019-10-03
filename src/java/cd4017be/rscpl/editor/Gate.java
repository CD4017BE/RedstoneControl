package cd4017be.rscpl.editor;

import java.util.function.IntFunction;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import static cd4017be.rscpl.editor.InvalidSchematicException.*;
import cd4017be.lib.util.Utils;
import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public class Gate {

	public final GateType type;
	public final int index;
	protected final Pin[] inputs;
	public final Pin[] outputs;
	/** -1: checking, 0: unchecked, 1: approved valid */
	public byte check = 0;
	public int rasterX, rasterY;
	public String label = "";
	public final TraceNode[] traces;

	public Gate(GateType type, int index, int in, int out) {
		this.type = type;
		this.index = index;
		this.inputs = new Pin[in];
		this.outputs = new Pin[out];
		for (int i = 0; i < outputs.length; i++)
			outputs[i] = new Pin(this, i);
		this.traces = new TraceNode[inputs.length];
	}

	public int inputCount() {
		return inputs.length;
	}

	public Pin getInput(int pin) {
		return inputs[pin];
	}

	public void setInput(int pin, Pin op) {
		Pin old = inputs[pin];
		if (old == op) return;
		Pair<Gate, Integer> p = Pair.of(this, pin);
		if (old != null) old.receivers.remove(p);
		inputs[pin] = op;
		if (op != null) op.receivers.add(p);
	}

	public void checkValid() throws InvalidSchematicException {
		if (check > 0) return;
		check = -1;
		for (Pin out : outputs)
			out.node = null;
		Pin[] inputs = this.inputs;
		for (int i = 0, l = inputs.length; i < l; i++) {
			Pin pin = inputs[i];
			if (pin != null) {
				if (!type.isInputTypeValid(i, pin.getOutType()))
					throw new InvalidSchematicException(TYPE_MISSMATCH, this, i);
				Gate gate = pin.gate;
				if (gate.check < 0)
					throw new InvalidSchematicException(CAUSAL_LOOP, this, i);
				gate.checkValid();
			} else if (!type.isInputTypeValid(i, Type.VOID_TYPE))
				throw new InvalidSchematicException(MISSING_INPUT, this, i);
		}
		check = 1;
	}

	public void read(ByteBuf data) {
		setPosition(data.readUnsignedByte(), data.readUnsignedByte());
		for (int i = 0; i < traces.length; i++) {
			TraceNode tn = new TraceNode(null, i);
			int n = data.readUnsignedByte();
			if (n == 0) {
				tn.rasterX = 256;
				tn.rasterY = 256;
			} else {
				tn.rasterX = data.readUnsignedByte();
				tn.rasterY = data.readUnsignedByte();
			}
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
			Pin op = inputs[i];
			int p = data.writerIndex(), n = 1;
			data.writeByte(0);
			data.markWriterIndex();
			if (op == null) {
				data.writeByte(-1);
				data.writeByte(-1);
			} else {
				data.writeByte(op.gate.index);
				data.writeByte(op.idx);
			}
			for (TraceNode tn = traces[i]; tn != null; tn = tn.next, n++) {
				data.writeByte(tn.rasterX);
				data.writeByte(tn.rasterY);
			}
			if (op != null || n > 1)
				data.setByte(p, n);
			else data.resetWriterIndex();
		}
		int i = data.writeByte(0).writeCharSequence(label, Utils.UTF8);
		data.setByte(data.writerIndex() - i - 1, i);
	}

	public void reconnect(IntFunction<Gate> indexTable) {
		for (int i = 0; i < traces.length; i++) {
			TraceNode tn = traces[i];
			if (tn != null && tn.owner == null) {
				Gate g = indexTable.apply(tn.rasterX);
				setInput(i, g == null ? null : g.outputs[tn.rasterY]);
				traces[i] = tn.next;
			}
		}
	}

	public void remove() {
		for (int i = 0; i < inputs.length; i++)
			setInput(i, null);
		for (Pin o : outputs) {
			for (Pair<Gate, Integer> p : o.receivers)
				p.getLeft().inputs[p.getRight()] = null;
			o.receivers.clear();
		}
	}

	public void setPosition(int x, int y) {
		rasterX = x;
		rasterY = y;
	}

	public BoundingBox2D<Gate> getBounds() {
		return new BoundingBox2D<>(this, rasterX, rasterY, Math.abs(type.width), type.height);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Integer.toHexString(index)).append(' ').append(type);
		if (!label.isEmpty()) sb.append(" \"").append(label).append('"');
		return sb.toString();
	}

	public TraceNode getTrace(int pin, int trace) {
		TraceNode tn = traces[pin];
		for (; trace > 0 && tn != null; trace--) tn = tn.next;
		return tn;
	}

}
