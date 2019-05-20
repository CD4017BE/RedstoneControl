package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItemBlock;
import cd4017be.rs_ctr.api.wire.IHookAttachable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class ItemWireAnchor extends BaseItemBlock {

	/**
	 * @param id
	 */
	public ItemWireAnchor(Block id) {
		super(id);
	}

	@Override
	public boolean canItemEditBlocks() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
		return true;
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		int p = IHookAttachable.getAttachmentPos(new Vec3d(hitX, hitY, hitZ), facing, player);
		int q = p & 15;
		if (q >= 4) pos = pos.add(2 - q/4, 0, 0);
		q = p >> 4 & 15;
		if (q >= 4) pos = pos.add(0, 2 - q/4, 0);
		q = p >> 8 & 15;
		if (q >= 4) pos = pos.add(0, 0, 2 - q/4);
		p &= 0x3f0333;
		ItemStack stack = player.getHeldItem(hand);
		if (!player.canPlayerEdit(pos, facing, stack))
			return EnumActionResult.FAIL;
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof IHookAttachable)) {
			IBlockState state = world.getBlockState(pos);
			if (!state.getBlock().isReplaceable(world, pos) ||
					!placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, block.getDefaultState()) ||
					!((te = world.getTileEntity(pos)) instanceof IHookAttachable))
				return EnumActionResult.FAIL;
		}
		IHookAttachable att = (IHookAttachable)te;
		if (att.doAttachHook(att.applyOrientation(p))) {
			stack.shrink(1);
			return EnumActionResult.SUCCESS;
		} else return EnumActionResult.FAIL;
	}

}
