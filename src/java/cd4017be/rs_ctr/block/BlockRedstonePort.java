package cd4017be.rs_ctr.block;

import cd4017be.lib.block.BlockCoveredPipe;
import cd4017be.lib.block.BlockPipe;
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.wire.IHookAttachable;
import cd4017be.rs_ctr.api.wire.RelayPort;
import cd4017be.rs_ctr.tileentity.RedstonePort;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
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
		super(id, m, sound, BlockPipe.CON_PROPS.length + 2, tile);
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
	public Class<?> moduleType(int i) {
		return i < 7 ? super.moduleType(i) : IBlockRenderComp[].class;
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
			if (res != null && ((RedstonePort)te).breakPort(res.subHit - 1, player, willHarvest))
				return false;
		}
		if (willHarvest) return true;
		return world.setBlockState(pos, net.minecraft.init.Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof RedstonePort)) return ItemStack.EMPTY;
		RedstonePort port = (RedstonePort)te;
		int i = target.subHit - 1;
		//if (i == 6 && target.hitVec.squareDistanceTo(0.5, 0.5, 0.5) < 0.7) i = target.sideHit.ordinal();
		if (i == -2) return new ItemStack(RelayPort.HOOK_ITEM);
		if (i >= 0 && i < 6) {
			MountedSignalPort p = (MountedSignalPort)port.getSignalPort(i);
			if (p == null) p = (MountedSignalPort)port.getSignalPort(i + 6);
			if (p != null) return new ItemStack(Objects.rs_port, 1, p.isMaster ? 0 : 1);
		}
		state = port.cover.state;
		if (state == null) return ItemStack.EMPTY;
		ItemStack stack = port.cover.stack;
		if (stack != null && !stack.isEmpty()) return stack;
		return state.getBlock().getPickBlock(state, target, world, pos, player);
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side) {
		return isSideSolid(state, world, pos, side) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
		return IHookAttachable.addBlockRayTrace(super.collisionRayTrace(state, world, pos, start, end), world, pos, start, end);
	}

}
