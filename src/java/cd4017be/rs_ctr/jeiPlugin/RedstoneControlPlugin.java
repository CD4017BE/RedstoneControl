package cd4017be.rs_ctr.jeiPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

import cd4017be.lib.util.ItemKey;
import cd4017be.rs_ctr.CommonProxy;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.gui.GuiAssembler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;


/**
 * @author CD4017BE
 *
 */
@JEIPlugin
public class RedstoneControlPlugin implements IModPlugin {

	@Override
	public void register(IModRegistry registry) {
		
		registry.addRecipeCatalyst(new ItemStack(Objects.assembler), CircuitMaterials.UID);
		registry.addRecipeClickArea(GuiAssembler.class, 84, 60, 9, 9, CircuitMaterials.UID);
		registry.handleRecipes(Entry.class, CircuitMatRecipe::new, CircuitMaterials.UID);
		ArrayList<Entry<ItemKey, int[]>> recipes = new ArrayList<>(CommonProxy.MATERIALS.entrySet());
		Collections.sort(recipes, (a, b)-> {
			int[] sa = a.getValue(), sb = b.getValue();
			return sa[0] + sa[1] + sa[2] - sb[0] - sb[1] - sb[2];
		});
		registry.addRecipes(recipes, CircuitMaterials.UID);
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {
		registry.addRecipeCategories(new CircuitMaterials(registry.getJeiHelpers().getGuiHelper()));
	}

}
