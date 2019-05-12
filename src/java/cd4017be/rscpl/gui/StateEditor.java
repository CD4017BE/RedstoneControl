package cd4017be.rscpl.gui;

import java.util.Arrays;
import java.util.function.IntConsumer;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.FrameGrip;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Slider;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.util.StateBuffer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ResourceLocation;
import static net.minecraftforge.common.util.Constants.NBT.*;

/**
 * @author CD4017BE
 *
 */
public class StateEditor extends GuiFrame {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/palette.png");

	private final String[] keys;
	public final StateBuffer state;
	public final IntConsumer set;
	int scroll;

	public static String[] getKeys(StateBuffer state) {
		String[] s = state.nbt.getKeySet().toArray(new String[state.nbt.getSize()]);
		Arrays.sort(s);
		return s;
	}

	/**
	 * @param parent
	 * @param w
	 * @param h
	 * @param comps
	 */
	public StateEditor(GuiFrame parent, int n, String[] keys, StateBuffer state, IntConsumer set) {
		super(parent, 96, 22 + n * 18, 2 * n + 1);
		this.state = state;
		this.set = set;
		this.keys = keys;
		texture(TEX, 256, 256);
		title("gui.circuits.state.name", 0.5F);
		new FrameGrip(this, 8, 8, 0, 0);
		if (keys.length > n)
			new Slider(this, 8, 12, n * 18 - 2, 80, 16, 248, 0, false, ()-> scroll, (v)-> scroll = (int)Math.round(v), null, 0, keys.length - n);
		for (int i = 0; i < n; i++) {
			final int idx = i;
			new FormatText(this, 70, 9, 8, 16 + i * 18, "\\%s", ()-> new Object[] {keys[idx + scroll]});
			new TextField(this, 70, 7, 8, 25 + i * 18, 16, ()-> getValue(idx), (t)-> setValue(idx, t));
		}
	}

	private String getValue(int i) {
		NBTBase tag = state.nbt.getTag(keys[i += scroll]);
		return tag.toString();
	}

	private void setValue(int i, String t) {
		String key = keys[i += scroll];
		try {
			switch(state.nbt.getTagId(key)) {
			case TAG_BYTE: state.set(key, (byte)parseNumber(t, Byte.MAX_VALUE)); break;
			case TAG_SHORT: state.set(key, (short)parseNumber(t, Short.MAX_VALUE)); break;
			case TAG_INT: state.set(key, (int)parseNumber(t, Integer.MAX_VALUE)); break;
			case TAG_LONG: state.set(key, parseNumber(t, Long.MAX_VALUE)); break;
			case TAG_FLOAT: state.set(key, Float.parseFloat(t)); break;
			case TAG_DOUBLE: state.set(key, Double.parseDouble(t)); break;
			default: return; //TODO arrays
			}
			set.accept(i);
		} catch (NumberFormatException e) {}
	}

	private long parseNumber(String t, long max) {
		if (t.isEmpty()) throw new NumberFormatException();
		char c = t.charAt(0);
		int rad = 10;
		boolean usgn = true;
		if (c == 'x') {
			rad = 16; t = t.substring(1);
		} else if (c == 'b') {
			rad = 2; t = t.substring(1);
		} else if (c != '+') {
			usgn = false;
		}
		long l;
		if (usgn) {
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
		parent.drawNow();
		parent.bindTexture(mainTex);
		gui.drawTexturedModalRect(x, y, 86, 0, w, h - 8);
		gui.drawTexturedModalRect(x, y + h - 8, 86, 122, w, 8);
		super.drawBackground(mx, my, t);
	}

}
