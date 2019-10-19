package cd4017be.rs_ctr.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

/** Special version of InventoryPlayer where all 36 main inventory slots can be selected as currently held item in main hand.
 * Used for FakePlayers.
 * @author CD4017BE */
public class FullHotbarInventory extends InventoryPlayer {

	public FullHotbarInventory(EntityPlayer playerIn) {
		super(playerIn);
	}

	@Override
	public ItemStack getCurrentItem() {
		return currentItem < mainInventory.size()
			? mainInventory.get(currentItem) : ItemStack.EMPTY;
	}

}
