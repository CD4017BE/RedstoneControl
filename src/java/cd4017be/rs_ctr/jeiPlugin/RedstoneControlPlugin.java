package cd4017be.rs_ctr.jeiPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;

import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.api.rs_ctr.sensor.SensorRegistry;
import cd4017be.lib.util.ItemKey;
import cd4017be.rs_ctr.CommonProxy;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.gui.GuiAssembler;
import mezz.jei.api.IGuiHelper;
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

	public static final String
			UID_CIRCUIT_MAT = "rs_ctr:circuit_mat",
			UID_BATTERIES = "rs_ctr:battery",
			UID_SENSORS = "rs_ctr:sensors";

	@Override
	public void register(IModRegistry registry) {
		{
			registry.addRecipeCatalyst(new ItemStack(Objects.assembler), UID_CIRCUIT_MAT);
			registry.addRecipeClickArea(GuiAssembler.class, 84, 60, 9, 9, UID_CIRCUIT_MAT);
			registry.handleRecipes(Entry.class, CircuitMatRecipe::new, UID_CIRCUIT_MAT);
			ArrayList<Entry<ItemKey, int[]>> recipes = new ArrayList<>(CommonProxy.MATERIALS.entrySet());
			Collections.sort(recipes, CircuitMatRecipe::compare);
			registry.addRecipes(recipes, UID_CIRCUIT_MAT);
		} {
			registry.addRecipeCatalyst(new ItemStack(Objects.power_hub), UID_BATTERIES);
			registry.handleRecipes(Entry.class, BatteryRecipe::new, UID_BATTERIES);
			ArrayList<Entry<ItemKey, Long>> recipes = new ArrayList<>(CommonProxy.BATTERIES.entrySet());
			Collections.sort(recipes, BatteryRecipe::compare);
			registry.addRecipes(recipes, UID_BATTERIES);
		} {
			registry.addRecipeCatalyst(new ItemStack(Objects.comparator), UID_SENSORS);
			registry.handleRecipes(Entry.class, SensorRecipe::new, UID_SENSORS);
			HashMap<IBlockSensor, ArrayList<ItemStack>> recipes = new HashMap<>();
			for (Entry<ItemKey, Function<ItemStack, IBlockSensor>> e : SensorRegistry.REGISTRY.entrySet()) {
				ItemStack stack = e.getKey().items[0];
				recipes.compute(e.getValue().apply(stack), (k, o)-> {
					if (o == null) o = new ArrayList<>(1);
					o.add(stack);
					return o;
				});
			}
			registry.addRecipes(recipes.entrySet(), UID_SENSORS);
		}
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {
		IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
		registry.addRecipeCategories(new CircuitMaterials(guiHelper, UID_CIRCUIT_MAT));
		registry.addRecipeCategories(new ItemStatCategory<>(guiHelper, UID_BATTERIES, 220));
		registry.addRecipeCategories(new ItemStatCategory<>(guiHelper, UID_SENSORS, 220));
	}

}
