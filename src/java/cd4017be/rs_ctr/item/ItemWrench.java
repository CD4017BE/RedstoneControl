package cd4017be.rs_ctr.item;

import java.util.HashSet;

import cd4017be.lib.item.BaseItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 
 * @author cd4017be
 */
public class ItemWrench extends BaseItem {

	public static final HashSet<ResourceLocation> WRENCHES = new HashSet<ResourceLocation>();

	public ItemWrench(String id) {
		super(id);
		WRENCHES.add(getRegistryName());
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {
		return true;
	}

	//TODO rotate blocks
}
