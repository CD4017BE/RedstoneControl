package cd4017be.rs_ctr.circuit.editor;

/** @author CD4017BE */
public enum Placement {
	FREE, LEFT, RIGHT;

	public int adjustX(int x, int w) {
		switch(this) {
		case LEFT:
			return 0;
		case RIGHT:
			return w;
		default:
			return x;
		}
	}

}
