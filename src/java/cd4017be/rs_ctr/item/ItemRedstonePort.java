package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.templates.Cover;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.tileentity.RedstonePort;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockStructure;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
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
		return true;
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		RedstonePort port = null;
		if (hand == EnumHand.OFF_HAND) port = replaceBlock(player, world, pos);
		else if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
			BlockPos pos1 = pos.offset(facing);
			TileEntity te = world.getTileEntity(pos1);
			if (te instanceof RedstonePort) {
				port = (RedstonePort)te;
				pos = pos1;
				facing = facing.getOpposite();
			}
		}
		
		if (port == null) return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
		ItemStack stack = player.getHeldItem(hand);
		if (stack.getItem() == this && player.canPlayerEdit(pos, facing, stack) && port.addPort(facing, stack.getMetadata())) {
			stack.shrink(1);
			return EnumActionResult.SUCCESS;
		} else return EnumActionResult.FAIL;
	}

	/**
	 * turn the block into a covered RedstonePort
	 */
	private RedstonePort replaceBlock(EntityPlayer player, World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof RedstonePort) return (RedstonePort)te;
		if (!Cover.isBlockValid(null, state)) return null;
		if (!player.isCreative() && (state.getPlayerRelativeBlockHardness(player, world, pos) <= 0
				|| !state.getBlock().canHarvestBlock(world, pos, player))) return null;
		
		// Logic from tryHarvestBlock for pre-canceling the event
		ItemStack stack = player.getHeldItemMainhand();
		Block block = state.getBlock();
		boolean preCancelEvent = player.isSpectator() || !player.isAllowEdit() && (stack.isEmpty() || !stack.canDestroy(block));
		
		// Post the block break event
		BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, player);
		event.setCanceled(preCancelEvent);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isCanceled()) return null;
		if ((block instanceof BlockCommandBlock || block instanceof BlockStructure) && !player.canUseCommandBlock()) return null;
		
		world.playEvent(player, 2001, pos, Block.getStateId(state));
		
		if (player.isCreative()) {
			if (!block.removedByPlayer(state, world, pos, player, false)) return null;
			block.onBlockDestroyedByPlayer(world, pos, state);
		} else {
			ItemStack itemstack2 = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
			if (!stack.isEmpty()) {
				stack.onBlockDestroyed(world, state, pos, player);
				if (stack.isEmpty()) ForgeEventFactory.onPlayerDestroyItem(player, itemstack2, EnumHand.MAIN_HAND);
			}
			if (!block.removedByPlayer(state, world, pos, player, true)) return null;
			block.onBlockDestroyedByPlayer(world, pos, state);
			block.harvestBlock(world, player, pos, state, te, itemstack2);
			// Drop experience
			int exp = event.getExpToDrop();
			if (exp > 0) block.dropXpOnBlockBreak(world, pos, exp);
		}
		
		world.setBlockState(pos, Objects.RS_PORT.getDefaultState(), 11);
		te = world.getTileEntity(pos);
		if (!(te instanceof RedstonePort)) return null;
		RedstonePort port = (RedstonePort)te;
		ItemStack coverStack = ItemStack.EMPTY;
		for (EntityItem ei : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos)))
			if (!ei.isDead && !(coverStack = ei.getItem()).isEmpty()) {
				world.removeEntity(ei);
				break;
			}
		port.cover.stack = coverStack;
		port.cover.state = state;
		port.cover.opaque = state.isOpaqueCube();
		return port;
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
		items.add(new ItemStack(this, 1, 2));
		items.add(new ItemStack(this, 1, 3));
	}

}
