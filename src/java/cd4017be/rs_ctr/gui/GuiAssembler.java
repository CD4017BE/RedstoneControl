package cd4017be.rs_ctr.gui;

import java.util.function.Supplier;

import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.InfoTab;
import cd4017be.lib.Gui.comp.SideSelector;
import cd4017be.lib.Gui.comp.Tooltip;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.tileentity.Assembler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GuiAssembler extends ModularGui implements Supplier<Object[]> {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/assembler.png");

	private final Assembler tile;

	public GuiAssembler(Assembler tile, EntityPlayer player) {
		super(tile.getContainer(player, 0));
		this.tile = tile;
		GuiFrame comps = new GuiFrame(this, 176, 168, 11)
		.background(TEX, 0, 0).title("gui.rs_ctr.assembler.name", 0.5F);
		new InfoTab(comps, 7, 8, 7, 6, "gui.rs_ctr.assembler.info");
		new FormatText(comps, 48, 54, 120, 16, editor == null ? "gui.rs_ctr.assembler.stats" : "gui.rs_ctr.assembler.stats1", this);
		for (int i = 0; i < 6; i++)
			new Tooltip(comps, 54, 8, 114, 16 + i * 9, "gui.rs_ctr.mat" + i, null);
		new SideSelector(comps, this, 36, 54, 7, 15, ()-> EnumFacing.HORIZONTALS[(int)(System.currentTimeMillis() / 500) & 3], null, null).type(SideSelector.T_IN);
		new SideSelector(comps, this, 18, 18, 79, 33, ()-> EnumFacing.DOWN, null, null).type(SideSelector.T_IN);
		new SideSelector(comps, this, 18, 18, 79, 15, ()-> EnumFacing.UP, null, null).type(SideSelector.T_IN).tooltip("gui.rs_ctr.template");
		compGroup = comps;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		if (tile.buff.items[6].isEmpty()) return;
		mc.renderEngine.bindTexture(TEX);
		GlStateManager.color(1, 1, 1, 1);
		drawTexturedModalRect(guiLeft + 80, guiTop + 34, 192, tile.step >= 0 ? 16 : 0, 16, 16);
		for (int i = 0; i < 3; i++)
			drawTexturedModalRect(guiLeft + 53, guiTop + 16 + i * 18, 176, tile.step > i ? 16 : 0, 16, 16);
	}

	@Override
	public Object[] get() {
		int[] stats = tile.inv.stats[0];
		int usage = Math.max(0, stats[0] + stats[1]);
		float burst = (float)stats[5] / (float)(usage - stats[4]);
		return new Object[] {
			stats[0], stats[1], stats[2], stats[3],
			burst < 0 ? Float.POSITIVE_INFINITY : burst,
			(float)stats[4] / (float)usage * 20F
		};
	}

}
