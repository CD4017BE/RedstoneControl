package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;


/**
 * @author CD4017BE
 *
 */
public class ItemChunkLoaderFuel extends BaseItem {

	/**
	 * @param id
	 */
	public ItemChunkLoaderFuel(String id) {
		super(id);
		setMaxDamage(1440);
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		int t = (item.getMaxDamage() - item.getItemDamage()) * item.getCount();
		return TooltipUtil.format(getUnlocalizedName() + ".name", t / 60, t % 60);
	}

	@Override
	public int getItemStackLimit(ItemStack stack) {
		return stack.isItemDamaged() ? 1 : maxStackSize;
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
		IBlockState state = player.world.getBlockState(pos);
		state.getBlock().onBlockClicked(player.world, pos, player);
		return true;
	}

}
