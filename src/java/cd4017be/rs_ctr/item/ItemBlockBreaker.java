package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItemBlock;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;

/** @author CD4017BE */
public class ItemBlockBreaker extends BaseItemBlock {

	public ItemBlockBreaker(Block id) {
		super(id);
	}

	@Override
	public boolean canApplyAtEnchantingTable(
		ItemStack stack, Enchantment enchantment
	) {
		return enchantment == Enchantments.SILK_TOUCH
			|| enchantment == Enchantments.FORTUNE;
	}

	@Override
	public int getItemEnchantability() {
		return 1;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return true;
	}

}
