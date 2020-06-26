package cd4017be.rs_ctr.circuit;

/** 
 * @author CD4017BE */
public class UtilFunc {

	public static float floor(float x) {
		int i = Float.floatToRawIntBits(x);
		int e = 150 - (i >> 23 & 0xff);
		if (e <= 0) return x;
		if (e > 23) return x >= 0 ? 0 : -1F;
		if (i >= 0) i = i >> e;
		else i = (i - 1 >> e) + 1;
		return Float.intBitsToFloat(i << e);
	}

	public static float ceil(float x) {
		int i = Float.floatToRawIntBits(x);
		int e = 150 - (i >> 23 & 0xff);
		if (e <= 0) return x;
		if (e > 23) return x <= 0 ? 0 : 1F;
		if (i < 0) i = i >> e;
		else i = (i - 1 >> e) + 1;
		return Float.intBitsToFloat(i << e);
	}

	public static float sqrt(float x) {
		return (float)Math.sqrt(x);
	}

}
