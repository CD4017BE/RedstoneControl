package cd4017be.rs_ctr.jeiPlugin;

import java.util.Map.Entry;

import cd4017be.lib.util.ItemKey;
import cd4017be.lib.util.TooltipUtil;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;


/**
 * @author CD4017BE
 *
 */
public class BatteryRecipe implements IRecipeWrapper {

	public final ItemStack ingred;
	public final long cap;

	public BatteryRecipe(Entry<ItemKey, Long> entry) {
		this.ingred = entry.getKey().items[0];
		this.cap = entry.getValue();
	}

	@Override
	public void getIngredients(IIngredients ingredients) {
		ingredients.setInput(ItemStack.class, ingred);
	}

	@Override
	public void drawInfo(Minecraft minecraft, int w, int h, int mouseX, int mouseY) {
		FontRenderer fr = minecraft.fontRenderer;
		String s = TooltipUtil.format("recipe.rs_ctr.capacity", (double)cap / 1000D);
		fr.drawString(s, (18 + w - fr.getStringWidth(s)) / 2, 5, 0xff808080);
	}

	public static int compare(Entry<ItemKey, Long> a, Entry<ItemKey, Long> b) {
		return (int)(a.getValue() - b.getValue());
	}

}
