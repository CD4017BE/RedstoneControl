package cd4017be.rs_ctr.render;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public interface ISpecialRenderComp {

	@SideOnly(Side.CLIENT)
	void renderSpecial(double x, double y, double z, float t, FontRenderer fr);

}
