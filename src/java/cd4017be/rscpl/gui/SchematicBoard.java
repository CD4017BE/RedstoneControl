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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
	PinRef selPin;
	private Gate<?> placing;

	public SchematicBoard(GuiFrame parent, int x, int y, Schematic schematic, Runnable update) {
		super(parent, schematic.BOARD_AREA.width() << 1, schematic.BOARD_AREA.height() << 1, x, y);
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
				for (int i = g.inputCount() - 1; i >= 0; i--)
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
		parent.bindTexture(GateTextureHandler.GATE_ICONS_LOC);
		for (BoundingBox2D<Gate<?>> part : parts)
			drawPart(part);
		if (placing != null) {
			placing.rasterX = (mx - x) / 4;
			placing.rasterY = (my - y) / 4;
			BoundingBox2D<Gate<?>> part = placing.getBounds();
			drawPart(part);
		}
		drawWires(mx, my);
		if (placing != null) {
			parent.drawNow();
			BoundingBox2D<Gate<?>> part = placing.getBounds();
			for (BoundingBox2D<Gate<?>> p : parts)
				if (part.overlapsWith(p))
					drawSelection(p, 0x80ff0000);
		} else if (selPart != null) {
			parent.drawNow();
			BoundingBox2D<Gate<?>> part = selPart;
			if (moveX != 0 || moveY != 0) {
				part = part.offset(moveX*2, moveY*2);
				for (BoundingBox2D<Gate<?>> p : parts)
					if (part.overlapsWith(p))
						drawSelection(p, 0x80ff0000);
			}
			drawSelection(part, part.enclosedBy(schematic.BOARD_AREA) ? 0x80c08000 : 0xffff0000);
		}
	}

	private void drawWires(int mx, int my) {
		GlStateManager.disableTexture2D();
		BufferBuilder vb = Tessellator.getInstance().getBuffer();
		vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		int r = 0x7f, g = 0, b = 0, a = 0xff;
		float z = parent.zLevel;
		for (PinRef pin : pins.values())
			if (pin.equals(selPin)) {
				vb.pos(x + pin.x * 4 + 2.5, y + pin.y * 4 + 2.5, z).color(r, g, b, a).endVertex();
				vb.pos(mx + 0.5F, my + 0.5F, z).color(r, g, b, a).endVertex();
			} else if (pin.link != null) {
				vb.pos(x + pin.x * 4 + 2.5, y + pin.y * 4 + 2.5, z).color(r, g, b, a).endVertex();
				vb.pos(x + pin.link.x * 4 + 2.5, y + pin.link.y * 4 + 2.5, z).color(r, g, b, a).endVertex();
			}
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture2D();
	}

	private void drawPart(BoundingBox2D<Gate<?>> part) {
		Gate<?> node = part.owner;
		if (node instanceof ISpecialRender)
			((ISpecialRender)node).draw(this);
		else {
			TextureAtlasSprite tex = GateTextureHandler.GATE_ICONS_TEX.getAtlasSprite(node.type.getIcon());
			int w = tex.getIconWidth(), h = tex.getIconHeight();
			int px = x + 2 + part.x0 + part.x1 - w/2, py = y + 2 + part.y0 + part.y1 - h/2;
			parent.drawRect(px, py, tex.getOriginX(), tex.getOriginY(), w, h);
		}
	}

	public void drawTinyText(String s, int x, int y, int w) {
		char[] cs = s.toCharArray();
		boolean scaled = cs.length > w;
		if (scaled) {
			int scale = (cs.length + w - 1) / w;
			GlStateManager.pushMatrix();
			GlStateManager.scale(1F/(float)scale, 1F/(float)scale, 1);
			x *= scale; y *= scale; y += (scale - 1) * 5 / 2;
			w *= scale;
		}
		x += (w - cs.length) * 2;
		for (char c : cs) {
			parent.gui.drawTexturedModalRect(x, y, c << 2 & 0xfc, 244 + (c >> 6) * 6, 4, 6);
			x += 4;
		}
		if (scaled) GlStateManager.popMatrix();
	}

	private void drawSelection(BoundingBox2D<Gate<?>> part, int c) {
		int x0 = x + 2 + part.x0 * 2,
			x1 = x + 2 + part.x1 * 2,
			y0 = y + 2 + part.y0 * 2,
			y1 = y + 2 + part.y1 * 2;
		Gui.drawRect(x0, y0, x1, y0 + 1, c);
		Gui.drawRect(x0 + 1, y1, x1 + 1, y1 + 1, c);
		Gui.drawRect(x0, y0 + 1, x0 + 1, y1 + 1, c);
		Gui.drawRect(x1, y0, x1 + 1, y1, c);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		mx = (mx - x) / 2;
		my = (my - y) / 2;
		if (b == 0) {
			if (placing != null && d != 1) {
				parent.gui.sendPkt(ADD_GATE, (byte)schematic.INS_SET.id(placing.type), (byte)(mx/2), (byte)(my/2));
				placing = null;
				return true;
			}
			if (d == 0) {
				selPart = findPart(mx-1, my-1);
				update.run();
				originX = mx/2;
				originY = my/2;
			} else if (d == 2 && selPart != null) unfocus();
			moveX = mx/2 - originX;
			moveY = my/2 - originY;
		} else if (b == 1 && d == 0) {
			PinRef pin = pins.get((mx/2) & 0xffff | (my/2) << 16);
			if (pin != null && pin.trace >= 0) selPin = pin;
			else if (selPin != null) {
				parent.gui.sendPkt(CONNECT, (byte)selPin.gate,
					pin == null ? (byte)-1 : (byte)pin.gate,
					(byte)(selPin.pin | (pin == null ? 0xf0 : pin.pin << 4)));
				selPin = null;
			}
			return true;
		} else if (b == 2 && d != 0) {
			BoundingBox2D<Gate<?>> part = findPart(mx-1, my-1);
			if (part != null) placing = part.owner.type.newGate(0);
			selPart = null;
			update.run();
		}
		return true;
	}

	@Override
	public void unfocus() {
		if (selPart != null && (moveX != 0 || moveY != 0)) {
			if (selPart.offset(moveX*2, moveY*2).enclosedBy(schematic.BOARD_AREA))
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