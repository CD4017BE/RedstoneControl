package cd4017be.rs_ctr.jeiPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class CircuitMaterials implements IRecipeCategory<IRecipeWrapper> {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/assembler.png");
	public static final String UID = "rs_ctr:circuit_mat";
	
	private final IGuiHelper guiHelper;

	public CircuitMaterials(IGuiHelper guiHelper) {
		this.guiHelper = guiHelper;
	}

	@Override
	public String getUid() {
		return UID;
	}

	@Override
	public String getTitle() {
		return TooltipUtil.translate("recipe.rs_ctr.circuit_mat");
	}

	@Override
	public String getModName() {
		return Main.ID;
	}

	@Override
	public IDrawable getBackground() {
		return guiHelper.createDrawable(TEX, 7, 238, 162, 18);
	}

	@Override
	public List<String> getTooltipStrings(int mouseX, int mouseY) {
		if ((mouseX -= 18) < 0)
			return Collections.emptyList();
		int i = Math.min(2, mouseX / 40) * 2 + mouseY / 9;
		return Arrays.asList(TooltipUtil.translate("gui.rs_ctr.mat" + i));
	}

	@Override
	public void setRecipe(IRecipeLayout layout, IRecipeWrapper recipeWrapper, IIngredients ingredients) {
		IGuiItemStackGroup items = layout.getItemStacks();
		items.init(0, true, 0, 0);
		items.set(ingredients);
	}

}
