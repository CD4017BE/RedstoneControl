package cd4017be.rscpl.gui;

import java.util.Arrays;
import java.util.function.IntConsumer;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Slider;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rscpl.util.StateBuffer;
import net.minecraft.nbt.*;
import static net.minecraftforge.common.util.Constants.NBT.*;

/**
 * @author CD4017BE
 *
 */
public class StateEditor extends GuiCompGroup {

	private final int maxVar;
	public final String[] keys;
	private StateBuffer state;
	public final Circuit circuit;
	public final IntConsumer set;
	public final boolean editIO;
	public boolean hex;
	int scroll;

	/**
	 * @param parent
	 * @param circuit
	 * @param ioLabels
	 * @param max
	 * @param set
	 * @return
	 */
	public static StateEditor of(GuiFrame parent, Circuit circuit, String[] ioLabels, int max, IntConsumer set, boolean editIO) {
		StateBuffer state = circuit.getState();
		String[] keys = state.nbt.getKeySet().toArray(new String[state.nbt.getSize()]);
		Arrays.sort(keys);
		return new StateEditor(parent, circuit, state, keys, ioLabels, Math.min(max, keys.length), set, editIO);
	}

	private StateEditor(GuiFrame parent, Circuit circuit, StateBuffer state, String[] keys, String[] ioLabels, int n, IntConsumer set, boolean editIO) {
		super(parent, 168, 19 + (circuit.inputs.length + circuit.outputs.length + n) * 18 + (n > 0 ? 12 : 0), 2 * (circuit.inputs.length + circuit.outputs.length + n) + 1);
		parent.extendBy(this);
		this.circuit = circuit;
		this.state = circuit.getState();
		this.set = set;
		this.keys = keys;
		this.maxVar = n;
		this.editIO = editIO;
		int y = 12, in = circuit.inputs.length;
		for (int i = 0; i < in; i++, y += 9) {
			final String label = ioLabels[i];
			final int idx = i;
			new FormatText(this, 70, 7, 8, y + 1, "\\" + label.replace("%", "%%"), null).color(0xff00007f);
			if (editIO) new TextField(this, 70, 7, 90, y + 1, 16, ()-> Integer.toString(this.circuit.inputs[idx], hex ? 16 : 10), (t)-> {try {
						int v = Integer.parseInt(t, hex ? 16 : 10);
						if (v == circuit.inputs[idx]) return;
						circuit.inputs[idx] = v;
						set.accept(-1 - idx);
					} catch (NumberFormatException e) {}});
			else new FormatText(this, 70, 7, 90, y + 1, "\\%s", ()-> new Object[] {Integer.toString(this.circuit.inputs[idx], hex ? 16 : 10)});
		}
		int out = circuit.outputs.length;
		for (int i = 0; i < out; i++, y += 9) {
			final String label = ioLabels[in + i];
			final int idx = i;
			new FormatText(this, 70, 7, 8, y + 1, "\\" + label.replace("%", "%%"), null).color(0xff007f00);
			new FormatText(this, 70, 7, 90, y + 1, "\\%s", ()-> new Object[] {Integer.toString(circuit.outputs[idx], hex ? 16 : 10)});
		}
		y += 12;
		if (keys.length > n)
			new Slider(this, 8, 12, n * 9 - 2, 152, y + 1, 178, 0, false, ()-> scroll, (v)-> scroll = (int)Math.round(v), null, 0, keys.length - n);
		for (int i = 0; i < n; i++, y += 9) {
			final int idx = i;
			new FormatText(this, 70, 7, 8, y + 1, "\\%s", ()-> new Object[] {keys[idx + scroll]});
			new TextField(this, 70, 7, 80, y + 1, 1024, ()-> getValue(idx), (t)-> setValue(idx, t));
		}
	}

	private String getValue(int i) {
		return toString(state.nbt.getTag(keys[i += scroll]), hex);
	}

	private static String toString(NBTBase nbt, boolean hex) {
		switch(nbt.getId()) {
		case TAG_BYTE: return String.format(hex ? "%02X" : "%d", ((NBTPrimitive)nbt).getByte());
		case TAG_SHORT: return String.format(hex ? "%04X" : "%d", ((NBTPrimitive)nbt).getShort());
		case TAG_INT: return String.format(hex ? "%08X" : "%d", ((NBTPrimitive)nbt).getInt());
		case TAG_LONG: return String.format(hex ? "%016X" : "%d", ((NBTPrimitive)nbt).getLong());
		case TAG_FLOAT: return String.format(hex ? "%a" : "%s", ((NBTPrimitive)nbt).getFloat());
		case TAG_DOUBLE: return String.format(hex ? "%a" : "%s", ((NBTPrimitive)nbt).getDouble());
		case TAG_BYTE_ARRAY: {
			byte[] arr = ((NBTTagByteArray)nbt).getByteArray();
			int l = arr.length, ll = hex ? Integer.toHexString(l - 1).length() : Integer.toString(l - 1).length();
			String fmt = hex ? "%0" + ll + "x:%02x " : "%d:%d ";
			StringBuilder sb = new StringBuilder((ll + 3) * l);
			for (int j = 0; j < l; j++)
				sb.append(String.format(fmt, j, arr[j] & 0xff));
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
		case TAG_INT_ARRAY: {
			int[] arr = ((NBTTagIntArray)nbt).getIntArray();
			int l = arr.length, ll = hex ? Integer.toHexString(l - 1).length() : Integer.toString(l - 1).length();
			String fmt = hex ? "%0" + ll + "x:%08x " : "%d:%d ";
			StringBuilder sb = new StringBuilder((ll + (hex ? 8 : 5)) * l);
			for (int j = 0; j < l; j++)
				sb.append(String.format(fmt, j, arr[j]));
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
		case TAG_LIST: {
			NBTTagList list = (NBTTagList)nbt;
			int l = list.tagCount(), ll = hex ? Integer.toHexString(l - 1).length() : Integer.toString(l - 1).length();
			String fmt = hex ? "%0" + ll + "x:%s " : "%d:%s ";
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < l; j++)
				sb.append(String.format(fmt, j, toString(list.get(j), hex)));
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
		default: return nbt.toString();
		}
	}

	private void setValue(int i, String t) {
		String key = keys[i += scroll];
		try {
			state.nbt.setTag(key, fromString(state.nbt.getTag(key), t, hex ? 16 : 10));
			circuit.setState(state);
			set.accept(i);
		} catch (NumberFormatException e) {}
	}

	private NBTBase fromString(NBTBase nbt, String t, int rad) {
		switch(nbt.getId()) {
		case TAG_BYTE: return new NBTTagByte((byte)parseNumber(t, Byte.MAX_VALUE, rad));
		case TAG_SHORT: return new NBTTagShort((short)parseNumber(t, Short.MAX_VALUE, rad));
		case TAG_INT: return new NBTTagInt((int)parseNumber(t, Integer.MAX_VALUE, rad));
		case TAG_LONG: return new NBTTagLong(parseNumber(t, Long.MAX_VALUE, rad));
		case TAG_FLOAT: return new NBTTagFloat(Float.parseFloat(t));
		case TAG_DOUBLE: return new NBTTagDouble(Double.parseDouble(t));
		case TAG_BYTE_ARRAY: {
			byte[] arr = ((NBTTagByteArray)nbt).getByteArray();
			for (int p = t.indexOf(':'), q = -1; p >= 0; p = t.indexOf(':', p + 1)) {
				int j = Integer.parseInt(t.substring(q + 1, p), rad);
				if (j >= arr.length) continue;
				if ((q = t.indexOf(' ', p)) < 0) q = t.length();
				arr[j] = (byte)Integer.parseInt(t.substring(p + 1, q), rad);
			}
		}	break;
		case TAG_INT_ARRAY: {
			int[] arr = ((NBTTagIntArray)nbt).getIntArray();
			for (int p = t.indexOf(':'), q = -1; p >= 0; p = t.indexOf(':', p + 1)) {
				int j = Integer.parseInt(t.substring(q + 1, p), rad);
				if (j >= arr.length) continue;
				if ((q = t.indexOf(' ', p)) < 0) q = t.length();
				arr[j] = Integer.parseInt(t.substring(p + 1, q), rad);
			}
		}	break;
		case TAG_LIST: {
			NBTTagList list = (NBTTagList)nbt;
			for (int p = t.indexOf(':'), q = -1; p >= 0; p = t.indexOf(':', p + 1)) {
				int j = Integer.parseInt(t.substring(q + 1, p), rad);
				if (j >= list.tagCount()) continue;
				if ((q = t.indexOf(' ', p)) < 0) q = t.length();
				list.set(j, fromString(list.get(j), t.substring(p + 1, q), rad));
			}
		}	break;
		}
		return nbt;
	}

	private static long parseNumber(String t, long max, int rad) {
		if (t.isEmpty()) throw new NumberFormatException();
		long l;
		if (rad == 16 || t.charAt(0) == '+') {
			l = Long.parseUnsignedLong(t, rad);
			if (max != Long.MAX_VALUE && (l < 0 || l > max * 2 + 1))
				throw new NumberFormatException();
		} else {
			l = Long.parseLong(t, rad);
			if (l > max || l < -1 - max)
				throw new NumberFormatException();
		}
		return l;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		int y = this.y;
		drawRect(x, y, 0, 165, 168, 12);
		y += 12;
		for (int n = circuit.inputs.length; n > 0; n--, y += 9)
			drawRect(x, y, 0, editIO ? 147 : 81, 168, 9);
		for (int n = circuit.outputs.length; n > 0; n--, y += 9)
			drawRect(x, y, 0, 156, 168, 9);
		if (maxVar > 0) {
			int m = maxVar * 9, m2 = m/2;
			drawRect(x, y, 0, 165, 168, 12 + m - m2);
			drawRect(x, y + 12 + m - m2, 0, 249 - m2, 168, 7 + m2);
			String title = TooltipUtil.translate("gui.rs_ctr.state.name");
			drawNow();
			fontRenderer.drawString(title, x + (w - fontRenderer.getStringWidth(title)) / 2, y + 2, 0x404040);
		} else {
			drawRect(x, y, 0, 249, 168, 7);
			drawNow();
		}
		String title = TooltipUtil.translate("gui.rs_ctr.state.io");
		fontRenderer.drawString(title, x + (w - fontRenderer.getStringWidth(title)) / 2, this.y + 2, 0x404040);
		super.drawBackground(mx, my, t);
	}

	public void update() {
		state = circuit.getState();
	}

}
