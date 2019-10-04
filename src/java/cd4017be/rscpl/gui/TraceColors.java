package cd4017be.rscpl.gui;

import org.objectweb.asm.Type;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.Pin;

/** @author CD4017BE */
public class TraceColors {

	final int[] colorPalette;

	/** @param colors {void, boolean, char, byte, short,
	 *        int, float, long, double, array, object} */
	public TraceColors(int... colors) {
		this.colorPalette = colors;
	}

	/** @return 0xRRGGBB color of a trace connecting the given input pin with its source */
	public int color(Gate gate, int pin) {
		Type t = Type.VOID_TYPE;
		eval: {
			if(gate == null || pin >= gate.inputCount())
				break eval;
			t = gate.type.getInType(pin);
			Pin p = gate.getInput(pin);
			if(p == null)
				break eval;
			Type src = p.getOutType();
			if(src.getSort() < t.getSort())
				t = src;
		}
		return color(t);
	}

	/** @return 0xRRGGBB color associated with the given signal type */
	public int color(Type type) {
		return colorPalette[Math.min(colorPalette.length - 1, type.getSort())];
	}

	public static TraceColors DEFAULT = new TraceColors(
		0x202020, 0x0000ff, 0x7f007f, 0x7f007f, 0x7f007f,
		0x7f0000, 0xff7f00, 0x7f0000, 0xff7f00, 0xbfbf00, 0xbfbfbf
	);

}
