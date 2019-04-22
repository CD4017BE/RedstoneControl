package cd4017be.rs_ctr.block;

import cd4017be.lib.block.BlockCoveredPipe;
import cd4017be.lib.block.BlockPipe;
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.tileentity.RedstonePort;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;


/**
 * @author CD4017BE
 *
 */
public class BlockRedstonePort extends BlockCoveredPipe {

	/**
	 * @param id
	 * @param m
	 * @param sound
	 * @param tile
	 */
	public BlockRedstonePort(String id, Material m, SoundType sound, Class<? extends TileEntity> tile) {
		super(id, m, sound, BlockPipe.CON_PROPS.length + 1, tile);
		boundingBox = new AxisAlignedBB[] {
			NULL_AABB,
			new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.125, 0.75),
			new AxisAlignedBB(0.25, 0.875, 0.25, 0.75, 1, 0.75),
			new AxisAlignedBB(0.25, 0.25, 0, 0.75, 0.75, 0.125),
			new AxisAlignedBB(0.25, 0.25, 0.875, 0.75, 0.75, 1),
			new AxisAlignedBB(0, 0.25, 0.25, 0.125, 0.75, 0.75),
			new AxisAlignedBB(0.875, 0.25, 0.25, 1, 0.75, 0.75),
			FULL_BLOCK_AABB
		};
		setSolid(BY_CONNECTION);
	}

	@Override
	protected PropertyInteger createBaseState() {
		return null;
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof RedstonePort && !world.isRemote) {
			RayTraceResult res = Utils.getHit(player, state, pos);
			if (res != null && ((RedstonePort)te).breakPort(res.subHit - 1, player))
				return false;
		}
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side) {
		return isSideSolid(state, world, pos, side) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
	}

}
