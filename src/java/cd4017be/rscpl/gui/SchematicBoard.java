package cd4017be.rscpl.gui;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;

import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.rscpl.editor.BoundingBox2D;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.editor.Schematic;
import static cd4017be.rscpl.editor.Schematic.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * @author CD4017BE
 *
 */
public class SchematicBoard extends GuiCompBase<GuiFrame> {

	private final Schematic schematic;
	private final Runnable update;
	public final ArrayList<BoundingBox2D<Gate<?>>> parts = new ArrayList<>();
	public final Int2ObjectOpenHashMap<PinRef> pins = new Int2ObjectOpenHashMap<>();
	public BoundingBox2D<Gate<?>> selPart;
	int originX, originY, moveX, moveY;
	boolean movingTrace;
	PinRef selPin;
	private Gate<?> placing;

	public SchematicBoard(GuiFrame parent, int x, int y, Schematic schematic, Runnable update) {
		super(parent, (schematic.BOARD_AREA.width() + 1) << 2, schematic.BOARD_AREA.height() << 2, x, y);
		this.schematic = schematic;
		this.update = update;
	}

	public void update() {
		if (!schematic.modified) return;
		parts.clear();
		pins.clear();
		for (Gate<?> g : schematic.operators)
			if (g != null) {
				parts.add(g.getBounds());
				for (int i = g.outputCount() - 1; i >= 0; i--) {
					PinRef pin = new PinRef(g.getOutput(i));
					pins.putIfAbsent(pin.hashCode(), pin);
				}
				for (int i = g.visibleInputs() - 1; i >= 0; i--)
					for (PinRef pin = new PinRef(g, i); pin != null; pin = pin.link)
						pins.put(pin.hashCode(), pin);
			}
		if (selPart != null) {
			Gate<?> op = schematic.get(selPart.owner.index);
			selPart = op == null ? null : op.getBounds();
			update.run();
		}
		schematic.modified = false;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		parent.bindTexture(parent.mainTex);
		parent.gui.mc.renderEngine.bindTexture(GateTextureHandler.GATE_ICONS_LOC);
		for (BoundingBox2D<Gate<?>> part : parts)
			drawPart(part);
		if (placing != null) {
			placing.setPosition((mx - x) / 4, (my - y) / 4);
			BoundingBox2D<Gate<?>> part = placing.getBounds();
			drawPart(part);
		}
		parent.drawNow();
		parent.bindTexture(null);
		drawWires(mx, my);
		if (placing != null) {
			BoundingBox2D<Gate<?>> part = placing.getBounds();
			for (BoundingBox2D<Gate<?>> p : parts)
				if (part.overlapsWith(p))
					drawSelection(p, 0x80ff0000);
		} else if (selPart != null) {
			BoundingBox2D<Gate<?>> part = selPart;
			if (moveX != 0 || moveY != 0) {
				part = part.offset(moveX, moveY);
				for (BoundingBox2D<Gate<?>> p : parts)
					if (part.overlapsWith(p))
						drawSelection(p, 0x80ff0000);
			}
			drawSelection(part, part.enclosedBy(schematic.BOARD_AREA) ? 0x80c08000 : 0xffff0000);
		}
		GlStateManager.color(1, 1, 1, 1);
	}

	private void drawWires(int mx, int my) {
		GlStateManager.disableTexture2D();
		BufferBuilder vb = Tessellator.getInstance().getBuffer();
		vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		int r = 0x7f, g = 0, b = 0, a = 0xff;
		float z = parent.zLevel + 0.5F;
		for (PinRef pin : pins.values()) {
			boolean sel = pin.equals(selPin);
			PinRef link = pin.link;
			if (link != null || sel && !movingTrace) {
				if (movingTrace && sel)
					vb.pos(mx + 0.5, my + 0.5, z).color(r, g, b, a).endVertex();
				else vb.pos(x + pin.x * 4 + 2.5, y + pin.y * 4 + 2.5, z).color(r, g, b, a).endVertex();
				if (movingTrace ? link.equals(selPin) : sel)
					vb.pos(mx + 0.5, my + 0.5, z).color(r, g, b, a).endVertex();
				else vb.pos(x + link.x * 4 + 2.5, y + link.y * 4 + 2.5, z).color(r, g, b, a).endVertex();
			}
		}
		GlStateManager.glLineWidth((float)parent.gui.mc.displayHeight / (float)parent.gui.height);
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture2D();
	}

	private void drawPart(BoundingBox2D<Gate<?>> part) {
		Gate<?> node = part.owner;
		int x = this.x + 2 + part.x0*4, y = this.y + part.y0*4;
		GateTextureHandler.drawIcon(parent.getDraw(), x, y, part.width()*4, part.height()*4, part.owner.type.getIcon(), parent.zLevel + 1);
		if (node instanceof ISpecialRender)
			((ISpecialRender)node).draw(this, x, y);
	}

	private void drawSelection(BoundingBox2D<Gate<?>> part, int c) {
		int x0 = x + 2 + part.x0 * 4,
			x1 = x + 2 + part.x1 * 4,
			y0 = y + part.y0 * 4,
			y1 = y + part.y1 * 4;
		Gui.drawRect(x0, y0, x1, y0 + 1, c);
		Gui.drawRect(x0 + 1, y1, x1 + 1, y1 + 1, c);
		Gui.drawRect(x0, y0 + 1, x0 + 1, y1 + 1, c);
		Gui.drawRect(x1, y0, x1 + 1, y1, c);
	}

	@Override
	public void drawOverlay(int mx, int my) {
		BoundingBox2D<Gate<?>> part = findPart((mx - x - 1) / 4, (my - y) / 4);
		if (part != null)
			parent.drawTooltip(part.owner.label, mx, my);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		mx = (mx - x - 1) / 4;
		my = (my - y) / 4;
		switch(b) {
		case B_LEFT:
			if (placing != null && d != A_HOLD) {
				parent.gui.sendPkt(ADD_GATE, (byte)schematic.INS_SET.id(placing.type), (byte)mx, (byte)my);
				placing = null;
				break;
			}
			if (d == A_DOWN) {
				selPart = findPart(mx, my);
				update.run();
				originX = mx;
				originY = my;
			} else if (d == A_UP && selPart != null) unfocus();
			moveX = mx - originX;
			moveY = my - originY;
			break;
		case B_RIGHT:
			if (d == A_DOWN) {
				PinRef pin = pins.get(mx & 0xffff | my << 16);
				if (selPin == null) {
					if (pin == null || pin.trace < 0) break;
					selPin = pin;
				} else if (pin == null) {
					selPin = pin = new PinRef(selPin, mx, my);
					parent.gui.sendPkt(INS_TRACE, (byte)pin.gate, (byte)(pin.pin | (pin.trace - 1) << 4), (byte)mx, (byte)my);
				} else if (pin.gate != selPin.gate || pin.pin != selPin.pin || pin.trace < 0){
					while(pin.link != null) pin = pin.link;
					parent.gui.sendPkt(CONNECT, (byte)selPin.gate, pin.trace >= 0 ? (byte)-1 : (byte)pin.gate, (byte)(selPin.pin | (pin.trace >= 0 ? 0xf0 : pin.pin << 4)), (byte)selPin.trace);
					selPin = null;
				} else if (pin.trace <= selPin.trace) {
					parent.gui.sendPkt(CONNECT, (byte)selPin.gate, (byte)-1, (byte)(selPin.pin | 0xf0), (byte)selPin.trace);
					selPin = null;
				} else {
					parent.gui.sendPkt(REM_TRACE, (byte)selPin.gate, (byte)selPin.pin, (byte)(selPin.trace & 0xf | (pin.trace - 1) << 4));
					selPin = null;
				}
			} else if (d == A_HOLD) {
				movingTrace = selPin != null && selPin.trace > 0;
			} else if (d == A_UP && movingTrace) {
				if (selPin != null && selPin.trace > 0) {
					PinRef pin = pins.get(mx & 0xffff | my << 16);
					if (pin == null)
						parent.gui.sendPkt(MOVE_TRACE, (byte)selPin.gate, (byte)(selPin.pin | (selPin.trace - 1) << 4), (byte)mx, (byte)my);
					selPin = null;
				}
				movingTrace = false;
			}
			break;
		case B_MID:
			if (d != A_DOWN) {
				BoundingBox2D<Gate<?>> part = findPart(mx, my);
				if (part != null) placing = part.owner.type.newGate(0);
				selPart = null;
				update.run();
			}
			break;
		}
		return true;
	}

	@Override
	public void unfocus() {
		if (selPart != null && (moveX != 0 || moveY != 0)) {
			if (selPart.offset(moveX, moveY).enclosedBy(schematic.BOARD_AREA))
				parent.gui.sendPkt(MOVE_GATE, (byte)selPart.owner.index,
					(byte)(selPart.owner.rasterX + moveX),
					(byte)(selPart.owner.rasterY + moveY));
			else parent.gui.sendPkt(REM_GATE, (byte)selPart.owner.index);
			originX += moveX;
			originY += moveY;
		}
	}

	@Override
	public boolean focus() {return true;}

	private BoundingBox2D<Gate<?>> findPart(int x, int y) {
		for (BoundingBox2D<Gate<?>> part : parts)
			if (part.isPointInside(x, y)) {
				return part;
			}
		return null;
	}

	public void place(GateType<?> op) {
		this.placing = op.newGate(0);
	}

	public void del(int b) {
		if (selPart != null)
			parent.gui.sendPkt(REM_GATE, (byte)selPart.owner.index);
	}

}