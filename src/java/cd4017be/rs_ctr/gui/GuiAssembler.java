package cd4017be.rs_ctr.gui;

import java.util.function.Supplier;

import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.InfoTab;
import cd4017be.lib.Gui.comp.Tooltip;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.tileentity.Assembler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GuiAssembler extends ModularGui implements Supplier<Object[]> {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/assembler.png");

	private final Assembler tile;

	/**
	 * @param container
	 */
	public GuiAssembler(Assembler tile, EntityPlayer player) {
		super(tile.getContainer(player, 0));
		this.tile = tile;
		GuiFrame comps = new GuiFrame(this, 176, 168, 1).background(TEX, 0, 0).title("gui.rs_ctr.assembler.name", 0.5F);
		new InfoTab(comps, 7, 8, 7, 6, "gui.rs_ctr.assembler.info");
		new FormatText(comps, 48, 54, 120, 16, "gui.rs_ctr.assembler.stats", this);
		for (int i = 0; i < 6; i++)
			new Tooltip(comps, 54, 8, 114, 16 + i * 9, "gui.rs_ctr.mat" + i, null);
		compGroup = comps;
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
