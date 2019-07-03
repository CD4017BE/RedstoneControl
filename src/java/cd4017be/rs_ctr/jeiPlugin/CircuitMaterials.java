package cd4017be.rs_ctr.jeiPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cd4017be.lib.util.TooltipUtil;
import mezz.jei.api.IGuiHelper;


/**
 * @author CD4017BE
 *
 */
public class CircuitMaterials extends ItemStatCategory<CircuitMatRecipe> {

	public CircuitMaterials(IGuiHelper guiHelper, String uid) {
		super(guiHelper, uid, 238);
	}

	@Override
	public List<String> getTooltipStrings(int mouseX, int mouseY) {
		if ((mouseX -= 18) < 0)
			return Collections.emptyList();
		int i = Math.min(2, mouseX / 40) * 2 + mouseY / 9;
		return Arrays.asList(TooltipUtil.translate("gui.rs_ctr.mat" + i));
	}

}
