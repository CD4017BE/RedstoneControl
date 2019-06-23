package cd4017be.rscpl.gui;

import java.util.function.Consumer;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.editor.BoundingBox2D;
import cd4017be.rscpl.editor.GateType;
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
			add(new Button(this, 16, 18, 1 + 16 * i, 57, 0, ()-> openTab == j ? 0 : 1, (b)-> openTab = j).texture(162, 0).tooltip("gategroup." + tabs[i].name.replace(':', '.')));
			tabs[i].arrange((w - 2) / 4, (h - 20) / 4);
		}
		position(x, y);
	}

	@Override
	public void drawOverlay(int mx, int my) {
		if (my >= y + 58) super.drawOverlay(mx, my);
		else {
			GateType<?> t = tabs[openTab].get((mx - x - 1) / 4, (my - y - 1) / 4);
			if (t != null)
				drawTooltip(getTooltip(t), mx, my);
		}
	}

	protected String getTooltip(GateType<?> t) {
		return TooltipUtil.translate("gate." + t.name.replace(':', '.'));
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		bound = false;
		super.drawBackground(mx, my, t);
		bindTexture(GateTextureHandler.GATE_ICONS_LOC);
		bound = true;
		for (int i = 0, x = this.x + 1, y = this.y + 59; i < tabs.length; i++, x += 16)
			GateTextureHandler.drawIcon(getDraw(), x, y, 16, 16, tabs[i].getIcon(), zLevel);
		int x = this.x + 3, y = this.y + 1;
		for (BoundingBox2D<GateType<?>> bb : tabs[openTab].instructions)
			GateTextureHandler.drawIcon(getDraw(), x + bb.x0*4, y + bb.y0*4, bb.width()*4 - 4, bb.height()*4, bb.owner.getIcon(), zLevel);
		drawNow();
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (my >= y + 56) return super.mouseIn(mx, my, b, d);
		GateType<?> t = tabs[openTab].get((mx - x - 1) / 4, (my - y - 1) / 4);
		if (t != null) pick.accept(t);
		return true;
	}

}
