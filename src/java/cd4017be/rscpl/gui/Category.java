package cd4017be.rscpl.gui;

import java.util.ArrayList;
import java.util.List;

import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.editor.BoundingBox2D;
import cd4017be.rscpl.editor.GateType;

/**
 * 
 * @author CD4017BE
 *
 */
public class Category {

	public final String name;
	public final List<BoundingBox2D<GateType<?>>> instructions = new ArrayList<>();
	private int lastWidth = 0;

	public Category(String name) {
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public <T extends GateType<T>> T[] add(T... types) {
		for (GateType<?> t : types)
			instructions.add(new BoundingBox2D<>(t, 0, 0, t.width + 1, t.height));
		return types;
	}

	public void arrange(int width, int height) {
		if (width == lastWidth) return;
		int x = 0, y = 0, my = Integer.MAX_VALUE;
		for (int l = instructions.size(), i = 0; i < l; i++) {
			BoundingBox2D<?> bb = instructions.get(i);
			bb.move(x - bb.x0, y - bb.y0);
			for(BoundingBox2D<?> b = collision(bb, i); b != null; b = collision(bb, i)) {
				if (b.y1 < my) my = b.y1;
				bb.move(b.x1 - bb.x0, 0);
			}
			if ((x = bb.x1) > width) {
				//start new line
				x = 0;
				y = my;
				my = Integer.MAX_VALUE;
				if (bb.x1 - bb.x0 > width) {
					width = bb.x1 - bb.x0;
					i = 0; y = -1;
					Main.LOG.warn("Gate {} is too wide for tab area! retrying arrangement with an increased width of {} ...", bb.owner, width);
				} else i--;
			} else if (bb.y1 < my)
				my = bb.y1;
		}
		if (my > height)
			Main.LOG.warn("Gate arrangement for tab {} exceeded height {} by {}!", name, height, my);
		lastWidth = width;
	}

	private BoundingBox2D<?> collision(BoundingBox2D<?> box, int count) {
		for (int i = 0; i < count; i++) {
			BoundingBox2D<?> b = instructions.get(i);
			if (b.overlapsWith(box)) return b;
		}
		return null;
	}

	public GateType<?> get(int x, int y) {
		for (BoundingBox2D<GateType<?>> bb : instructions)
			if (bb.isPointInside(x, y))
				return bb.owner;
		return null;
	}

	public String getIcon() {
		return name;
	}

}