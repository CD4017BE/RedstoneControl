package cd4017be.rs_ctr.circuit.data;

import java.lang.reflect.Array;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Type.*;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Spinner;
import cd4017be.lib.Gui.comp.TextField;
import io.netty.buffer.ByteBuf;

/** @author CD4017BE */
public class ArrayEditor implements GateConfiguration<Object> {

	public static final int MAX_ARRAY_LENGTH = 256;
	public static final ArrayEditor BYTE_ARRAY = new ArrayEditor(new byte[0]);
	public static final ArrayEditor SHORT_ARRAY = new ArrayEditor(new short[0]);
	public static final ArrayEditor INT_ARRAY = new ArrayEditor(new int[0]);
	public static final ArrayEditor FLOAT_ARRAY = new ArrayEditor(new float[0]);

	final Object emptyArray;
	public final Type type;

	public ArrayEditor(Object array) {
		this.type = Type.getType(array.getClass().getComponentType());
		this.emptyArray = array;
	}

	@Override
	public Object init() {
		return emptyArray;
	}

	@Override
	public void write(ByteBuf data, Object cfg) {
		data.writeShort(Array.getLength(cfg));
		switch(type.getSort()) {
		case BYTE:
			data.writeBytes((byte[])cfg);
			return;
		case SHORT:
			for (short s : (short[])cfg)
				data.writeShort(s);
			return;
		case INT:
			for (int s : (int[])cfg)
				data.writeInt(s);
			return;
		case FLOAT:
			for (float s : (float[])cfg)
				data.writeFloat(s);
			return;
		case LONG:
			for (long s : (long[])cfg)
				data.writeLong(s);
			return;
		case DOUBLE:
			for (double s : (double[])cfg)
				data.writeDouble(s);
			return;
		}
	}

	@Override
	public Object read(ByteBuf data) {
		int l = data.readUnsignedShort();
		if (l > MAX_ARRAY_LENGTH) return emptyArray;
		switch(type.getSort()) {
		case BYTE: {
			byte[] arr = new byte[l];
			data.readBytes(arr);
			return arr;
		}
		case SHORT: {
			short[] arr = new short[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readShort();
			return arr;
		}
		case INT: {
			int[] arr = new int[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readInt();
			return arr;
		}
		case FLOAT: {
			float[] arr = new float[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readFloat();
			return arr;
		}
		case LONG: {
			long[] arr = new long[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readLong();
			return arr;
		}
		case DOUBLE: {
			double[] arr = new double[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readDouble();
			return arr;
		}
		default: return null;
		}
	}

	@Override
	public int setupCfgGUI(
		GuiFrame gui, int y, Supplier<Object> get, Consumer<Object> set, String id
	) {
		Edit ed = new Edit(get, set);
		new TextField(
			gui, 26, 7, 1, y + 1, Integer.toString(MAX_ARRAY_LENGTH).length(),
			()-> Integer.toString(ed.getLength()), (t)-> {
				try {
					ed.setLenght(Integer.parseInt(t));
				} catch(NumberFormatException e) {}
			}
		).tooltip("gui.rs_ctr.array_len");
		Spinner s = new Spinner(gui, 30, 8, 28, y + 1, true, "\\%.0f", ed, ed, 0, MAX_ARRAY_LENGTH, 1).tooltip("gui.rs_ctr.array_idx");
		((Button)s.get(0)).texture(168, 80);
		((Button)s.get(1)).texture(173, 80);
		new Button(gui, 18, 9, 58, y, 2, ed, ed).texture(168, 166).tooltip("gui.rs_ctr.hex#");
		new TextField(gui, 74, 7, 1, y + 10, 20, ed, ed).tooltip("gui.rs_ctr.value");
		return y + 18;
	}

	class Edit implements DoubleSupplier, DoubleConsumer, IntSupplier, IntConsumer, Supplier<String>, Consumer<String> {

		final Supplier<Object> get;
		final Consumer<Object> set;
		public int index;
		public boolean hex;

		public Edit(Supplier<Object> get, Consumer<Object> set) {
			this.get = get;
			this.set = set;
		}

		public int getLength() {
			return Array.getLength(get.get());
		}

		public void setLenght(int l) {
			if (l < 0 || l > MAX_ARRAY_LENGTH) return;
			Object old = get.get();
			Object arr = Array.newInstance(old.getClass().getComponentType(), l);
			System.arraycopy(old, 0, arr, 0, Math.min(l, Array.getLength(old)));
			set.accept(arr);
		}

		@Override
		public void accept(String t) {
			Object arr = get.get();
			if (index >= Array.getLength(arr)) return;
			Number n;
			try {
				n = parse(t, hex);
			} catch (NumberFormatException e) {
				return;
			}
			switch(type.getSort()) {
			case BYTE:
				Array.setByte(arr, index, n.byteValue());
				break;
			case SHORT:
				Array.setShort(arr, index, n.shortValue());
				break;
			case INT:
				Array.setInt(arr, index, n.intValue());
				break;
			case FLOAT:
				Array.setFloat(arr, index, n.floatValue());
				break;
			case LONG:
				Array.setLong(arr, index, n.longValue());
				break;
			case DOUBLE:
				Array.setDouble(arr, index, n.doubleValue());
				break;
			}
			set.accept(arr);
		}

		@Override
		public String get() {
			Object arr = get.get();
			if (index >= Array.getLength(arr)) return "";
			return format((Number)Array.get(arr, index), hex);
		}

		@Override
		public void accept(int value) {
			hex = value != 0;
		}

		@Override
		public int getAsInt() {
			return hex ? 1 : 0;
		}

		@Override
		public void accept(double value) {
			index = (int)value;
			int l = getLength();
			if (index >= l) index = l - 1;
			if (index < 0) index = 0;
		}

		@Override
		public double getAsDouble() {
			return index;
		}

	}

	public static Number parse(String s, boolean hex) throws NumberFormatException {
		if (s.indexOf('.') >= 0)
			return Double.valueOf(s);
		return Long.parseLong(s, hex ? 16 : 10);
	}

	public static String format(Number x, boolean hex) {
		if (hex) return String.format(x instanceof Double || x instanceof Float ? "%a": "%x", x);
		return x.toString();
	}

}
