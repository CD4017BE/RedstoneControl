package cd4017be.rscpl.gui;

import java.util.function.Consumer;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.editor.BoundingBox2D;
import cd4017be.rscpl.editor.GateType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GatePalette extends GuiFrame {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/palette.png");
	private final Consumer<GateType<?>> pick;
	private final Category[] tabs;
	private int openTab;

	public GatePalette(GuiFrame parent, Category[] tabs, int x, int y, Consumer<GateType<?>> pick) {
		super(parent, 162, 76, tabs.length);
		this.tabs = tabs;
		this.pick = pick;
		background(TEX, 0, 0);
		this.titleY = -11;
		for (int i = 0; i < tabs.length; i++) {
			final int j = i;
			add(new Button(this, 16, 18, 1 + 16 * i, 57, 0, ()-> openTab == j ? 1 : 0, (b)-> openTab = j).texture(162, 0).tooltip("gategroup." + tabs[i].name.replace(':', '.')));
			tabs[i].arrange((w - 2) / 4, (h - 20) / 4);
		}
	}

	@Override
	public void drawOverlay(int mx, int my) {
		if (my >= 58) super.drawOverlay(mx, my);
		else {
			GateType<?> t = tabs[openTab].get((mx - x - 1) / 4, (my - y - 1) / 4);
			if (t != null)
				drawTooltip(TooltipUtil.translate("gate." + t.name.replace(':', '.')), mx, my);
		}
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		super.drawBackground(mx, my, t);
		bindTexture(GateTextureHandler.GATE_ICONS_LOC);
		for (int i = 0, x = this.x + 1, y = this.y + 59; i < tabs.length; i++, x += 16) {
			TextureAtlasSprite tex = GateTextureHandler.GATE_ICONS_TEX.getAtlasSprite(tabs[i].getIcon());
			int w = tex.getIconWidth(), h = tex.getIconHeight();
			drawRect(x + (16 - w)/2, y + (16 - h)/2, tex.getOriginX(), tex.getOriginY(), w, h);
		}
		int x = this.x + 2, y = this.y + 2;
		for (BoundingBox2D<GateType<?>> bb : tabs[openTab].instructions) {
			TextureAtlasSprite tex = GateTextureHandler.GATE_ICONS_TEX.getAtlasSprite(bb.owner.getIcon());
			int w = tex.getIconWidth(), h = tex.getIconHeight();
			drawRect(x + (bb.x0 + bb.x1)*2 - w/2, y + (bb.y0 + bb.y1)*2 - h/2, tex.getOriginX(), tex.getOriginY(), w, h);
		}
		drawNow();
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (super.mouseIn(mx, my, b, d)) return true;
		GateType<?> t = tabs[openTab].get((mx - x - 1) / 4, (my - y - 1) / 4);
		if (t == null) return false;
		pick.accept(t);
		return true;
	}

}
