package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

/** @author cd4017be */
public class ItemRAM extends BaseItemBlock {

	public ItemRAM(Block id) {
		super(id);
		setMaxStackSize(1);
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		int m = item.getMetadata();
		return TooltipUtil.format(
			this.getUnlocalizedName(item) + ".name",
			m < 5 ? Integer.toString(32 << m) : Integer.toString(1 << m - 5) + "k"
		);
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {
		int m = stack.getMetadata();
		return m >= 10 ? EnumRarity.EPIC
			: m >= 8 ? EnumRarity.RARE
			: m >= 6 ? EnumRarity.UNCOMMON
			: EnumRarity.COMMON;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (!isInCreativeTab(tab)) return;
		items.add(new ItemStack(this, 1, 5));
		items.add(new ItemStack(this, 1, 7));
		items.add(new ItemStack(this, 1, 9));
		items.add(new ItemStack(this, 1, 11));
		items.add(new ItemStack(this, 1, 13));
	}

	@Override
	public NBTTagCompound getNBTShareTag(ItemStack stack) {
		return null;//avoid unnecessarily sending many kilobytes of data to client
	}

}
