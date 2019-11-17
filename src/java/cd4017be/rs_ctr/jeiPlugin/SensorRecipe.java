package cd4017be.rs_ctr.jeiPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;


/**
 * @author CD4017BE
 *
 */
public class SensorRecipe implements IRecipeWrapper {

	final List<ItemStack> ingreds;
	final IBlockSensor sensor;

	public SensorRecipe(Entry<IBlockSensor, List<ItemStack>> entry) {
		this.ingreds = entry.getValue();
		this.sensor = entry.getKey();
	}

	@Override
	public void getIngredients(IIngredients ingredients) {
		ingredients.setInputLists(ItemStack.class, Collections.singletonList(ingreds));
	}

	@Override
	public void drawInfo(Minecraft minecraft, int w, int h, int mouseX, int mouseY) {
		FontRenderer fr = minecraft.fontRenderer;
		String[] ts = sensor.getTooltipString().split("\n");
		int y = (19 - fr.FONT_HEIGHT * ts.length) / 2;
		for (String s : ts) {
			fr.drawString(s, (18 + w - fr.getStringWidth(s)) / 2, y, 0xff404040);
			y += fr.FONT_HEIGHT;
		}
	}

}
