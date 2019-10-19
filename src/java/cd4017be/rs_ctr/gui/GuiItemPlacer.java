package cd4017be.rs_ctr.gui;

import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.tileentity.ItemPlacer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

/** @author CD4017BE */
public class GuiItemPlacer extends ModularGui {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/item_placer.png");

	private final ItemPlacer tile;

	public GuiItemPlacer(ItemPlacer tile, EntityPlayer player) {
		super(tile.getContainer(player, 0));
		this.tile = tile;
		this.compGroup = new GuiFrame(this, 194, 206, 1)
		.background(TEX, 0, 0).title("gui.rs_ctr.item_placer.name", 0.45F);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(
		float partialTicks, int mouseX, int mouseY
	) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		int slot = tile.aim >> 16;
		mc.renderEngine.bindTexture(TEX);
		GlStateManager.color(1, 1, 1, 1);
		drawTexturedModalRect(guiLeft + 25 + slot % 9 * 18, guiTop + (slot < 9 ? 83 : 7 + slot / 9 * 18), 194, 0, 18, 18);
		slot = tile.aim >> 8 & 0xff;
		drawTexturedModalRect(guiLeft + 169 + (slot & 15), guiTop + 22 - (slot >> 4), 212, 0, 3, 3);
		slot = tile.aim & 15;
		drawTexturedModalRect(guiLeft + 152, guiTop + 8, 240, slot * 16, 16, 16);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		super.drawGuiContainerForegroundLayer(mx, my);
		boolean blink = System.currentTimeMillis() % 1000 >= 500;
		mx -= guiLeft + 7;
		my -= guiTop + 7;
		if (mx < 0 || mx >= 180 || my < 0 || my >= 94 || my >= 72 && my < 76) return;
		Orientation o = tile.getOrientation();
		EnumFacing side;
		if (mx < 18)
			side = my >= 76 ? o.back : (blink ? o.back : o.front).rotateY();
		else if (my < 18) return;
		else {
			if (my >= 76) my -= 76;
			int slot = (mx - 18) / 18 + my / 18 * 9;
			side = slot == tile.aim >> 16 ? o.front :
				blink ? EnumFacing.UP : EnumFacing.DOWN;
		}
		drawSideConfig(side, 1);
	}

}
