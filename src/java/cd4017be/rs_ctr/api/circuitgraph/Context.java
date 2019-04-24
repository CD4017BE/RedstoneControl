package cd4017be.rs_ctr.api.circuitgraph;

import java.util.BitSet;

import org.objectweb.asm.Type;

/**
 * @author CD4017BE
 *
 */
public class Context {

	public static final int THIS_IDX = 0, DIRTY_IDX = 1, IO_IDX = 2;
	private final BitSet usedLocals;

	public Context() {
		usedLocals = new BitSet();
		usedLocals.set(THIS_IDX);
		usedLocals.set(DIRTY_IDX);
		usedLocals.set(IO_IDX);
	}

	/**
	 * @param t value type to store
	 * @return a newly allocated local variable index sufficient to hold a value of given type
	 */
	public int newLocal(Type t) {
		int i;
		switch(t.getSize()) {
		case 1:
			i = usedLocals.nextClearBit(0);
			usedLocals.set(i);
			break;
		case 2:
			i = -2;
			do {
				i = usedLocals.nextClearBit(i + 2);
			} while(usedLocals.get(i + 1));
			usedLocals.set(i);
			usedLocals.set(i + 1);
			break;
		default:
			throw new IllegalArgumentException("invalid type!");
		}
		return i;
	}

	/**
	 * frees the given local variable for reuse
	 * @param idx variable index
	 * @param t the type that was stored
	 */
	public void releaseLocal(int idx, Type t) {
		usedLocals.clear(idx);
		if (t.getSize() > 1)
			usedLocals.clear(idx + 1);
	}

}
