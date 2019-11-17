package cd4017be.rs_ctr.item;

import java.util.HashSet;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 
 * @author cd4017be
 */
public class ItemWrench extends BaseItem {

	public static final HashSet<ResourceLocation> WRENCHES = new HashSet<ResourceLocation>();

	public ItemWrench(String id) {
		super(id);
		WRENCHES.add(getRegistryName());
		setMaxStackSize(1);
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {
		return true;
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
		IBlockState state = player.world.getBlockState(pos);
		state.getBlock().onBlockClicked(player.world, pos, player);
		return true;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking()) {
			stack.setTagCompound(null);
			player.sendStatusMessage(new TextComponentTranslation("msg.rs_ctr.copy_clr"), true);
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}
		return new ActionResult<>(EnumActionResult.PASS, stack);
	}

	@Override
	public EnumActionResult onItemUse(
		EntityPlayer player, World world, BlockPos pos, EnumHand hand,
		EnumFacing facing, float hitX, float hitY, float hitZ
	) {
		IBlockState state = world.getBlockState(pos);
		return state.getBlock().rotateBlock(
			world, pos, player.isSneaking() ? facing.getOpposite() : facing
		) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return TooltipUtil.translate(this.getUnlocalizedName(item) + (item.hasTagCompound() ? ".name1" : ".name"));
	}

}
