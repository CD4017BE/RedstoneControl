package cd4017be.rscpl.editor;


/**
 * 
 * @author CD4017BE
 */
public class BoundingBox2D<T> {

	public final T owner;
	/** coordinates of upper left x0,y0 and lower right x1,y1 corner (x0 < x1, y0 < y1) **/
	public int x0, x1, y0, y1;

	/**
	 * @param owner object this bounding box belongs to
	 */
	public BoundingBox2D(T owner) {
		this.owner = owner;
	}

	public BoundingBox2D(T owner, int x, int y, int w, int h) {
		this(owner);
		this.x0 = x;
		this.x1 = x + w;
		this.y0 = y;
		this.y1 = y + h;
	}

	/**
	 * @param x floor-rounded X-coordinate of point
	 * @param y floor-rounded Y-coordinate of point
	 * @return whether the given point is inside
	 */
	public boolean isPointInside(int x, int y) {
		return x >= x0
			&& x < x1
			&& y >= y0
			&& y < y1;
	}

	/**
	 * @param box
	 * @return whether given box overlaps (not just touching) with this one
	 */
	public boolean overlapsWith(BoundingBox2D<?> box) {
		return box.x1 > x0
			&& box.x0 < x1
			&& box.y1 > y0
			&& box.y0 < y1
			&& box.owner != owner;
	}

	/**
	 * @param box
	 * @return whether this box is fully inside the given box
	 */
	public boolean enclosedBy(BoundingBox2D<?> box) {
		return x0 >= box.x0
			&& x1 <= box.x1
			&& y0 >= box.y0
			&& y1 <= box.y1;
	}

	/**
	 * @param dx delta X
	 * @param dy delta Y
	 */
	public void move(int dx, int dy) {
		x0 += dx; x1 += dx;
		y0 += dy; y1 += dy;
	}

	/**
	 * @param dx delta X
	 * @param dy delta Y
	 * @return a shifted version of this
	 */
	public BoundingBox2D<T> offset(int dx, int dy) {
		return new BoundingBox2D<T>(owner, x0 + dx, y0 + dy, x1 - x0, y1 - y0);
	}

	public int width() {
		return x1 - x0;
	}

	public int height() {
		return y1 - y0;
	}

}
