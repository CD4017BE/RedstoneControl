package cd4017be.rs_ctr.jeiPlugin;

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
public class ItemStatCategory<T extends IRecipeWrapper> implements IRecipeCategory<T> {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/assembler.png");
	private final IGuiHelper guiHelper;
	public final String uid;
	private final int bgY;

	public ItemStatCategory(IGuiHelper guiHelper, String uid, int bgY) {
		this.guiHelper = guiHelper;
		this.uid = uid;
		this.bgY = bgY;
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public String getTitle() {
		return TooltipUtil.translate("recipe." + uid.replace(':', '.'));
	}

	@Override
	public String getModName() {
		return Main.ID;
	}

	@Override
	public IDrawable getBackground() {
		return guiHelper.createDrawable(TEX, 7, bgY, 162, 18);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, T wrapper, IIngredients ingredients) {
		IGuiItemStackGroup items = layout.getItemStacks();
		items.init(0, true, 0, 0);
		items.set(ingredients);
	}

}
