package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.templates.Cover;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.tileentity.RedstonePort;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class ItemRedstonePort extends BaseItemBlock {

	/**
	 * @param id
	 */
	public ItemRedstonePort(Block id) {
		super(id);
		setHasSubtypes(true);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
		if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
			pos = pos.offset(side);
			if (world.getTileEntity(pos) instanceof RedstonePort) return true;
		}
		return world.mayPlace(this.block, pos, false, side, (Entity)null);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		IBlockState state = world.getBlockState(pos);
		if (hand == EnumHand.OFF_HAND && Cover.isBlockValid(null, state)) {
			EnumActionResult res = EnumActionResult.FAIL;
			if (player.canPlayerEdit(pos, facing, stack) && (player.isCreative() 
					|| state.getPlayerRelativeBlockHardness(player, world, pos) > 0
					&& state.getBlock().canHarvestBlock(world, pos, player))) {
				NonNullList<ItemStack> drop = NonNullList.create();
				state.getBlock().getDrops(drop, world, pos, state, 0);
				world.setBlockState(pos, Objects.RS_PORT.getDefaultState(), 11);
				if (!player.isCreative()) {
					ItemStack item = player.getHeldItemMainhand(), itemstack2 = item.copy();
					item.onBlockDestroyed(world, state, pos, player);
					if (item.isEmpty()) ForgeEventFactory.onPlayerDestroyItem(player, itemstack2, EnumHand.MAIN_HAND);
				}
				TileEntity te = world.getTileEntity(pos);
				if (te instanceof RedstonePort && ((RedstonePort)te).addPort(facing, stack.getMetadata())) {
					Cover c = ((RedstonePort)te).cover;
					c.stack = drop.isEmpty() ? ItemStack.EMPTY : drop.remove(0);
					c.state = state;
					c.opaque = state.isOpaqueCube();
					stack.shrink(1);
					res = EnumActionResult.SUCCESS;
				}
				if (!player.isCreative())
					for (ItemStack item : drop)
						ItemFluidUtil.dropStack(item, player);
			}
			return res;
		} else if (hand == EnumHand.OFF_HAND) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof RedstonePort)
				if (player.canPlayerEdit(pos, facing, stack) && ((RedstonePort)te).addPort(facing, stack.getMetadata())) {
					stack.shrink(1);
					return EnumActionResult.SUCCESS;
				} else return EnumActionResult.FAIL;
		}
		if (!state.getBlock().isReplaceable(world, pos)) {
			BlockPos pos1 = pos.offset(facing);
			TileEntity te = world.getTileEntity(pos1);
			if (te instanceof RedstonePort) {
				if (player.canPlayerEdit(pos1, facing, stack) && ((RedstonePort)te).addPort(facing.getOpposite(), stack.getMetadata())) {
					stack.shrink(1);
					return EnumActionResult.SUCCESS;
				} else return EnumActionResult.FAIL;
			}
		}
		return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
		if (super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
			TileEntity te = world.getTileEntity(pos);
			return te instanceof RedstonePort && ((RedstonePort)te).addPort(side.getOpposite(), stack.getMetadata());
		} else return false;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (!isInCreativeTab(tab)) return;
		items.add(new ItemStack(this, 1, 0));
		items.add(new ItemStack(this, 1, 1));
	}

}
