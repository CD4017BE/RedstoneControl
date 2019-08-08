package cd4017be.rs_ctr.block;

import cd4017be.api.rs_ctr.wire.IHookAttachable;
import cd4017be.api.rs_ctr.wire.RelayPort;
import cd4017be.lib.block.AdvancedBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class BlockWireAnchor extends AdvancedBlock {

	/**
	 * @param id
	 * @param m
	 * @param sound
	 * @param flags
	 * @param tile
	 */
	public BlockWireAnchor(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile) {
		super(id, m, sound, flags, tile);
		setBlockBounds(NULL_AABB);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
		double x0 = 1, x1 = 0, y0 = 1, y1 = 0, z0 = 1, z1 = 0;
		TileEntity te = world.getTileEntity(pos);
		IBlockState cover = te instanceof ICoverableTile ? ((ICoverableTile)te).getCover().state : null;
		if (cover != null) {
			AxisAlignedBB box = cover.getBoundingBox(world, pos);
			x0 = box.minX + 0.03125; x1 = box.maxX - 0.03125;
			y0 = box.minY + 0.03125; y1 = box.maxY - 0.03125;
			z0 = box.minZ + 0.03125; z1 = box.maxZ - 0.03125;
		}
		if (te instanceof IHookAttachable)
			for (RelayPort port : ((IHookAttachable)te).getHookPins().values()) {
				if (!port.isMaster) continue;
				double d = port.pos.x;
				if (d < x0) x0 = d;
				if (d > x1) x1 = d;
				d = port.pos.y;
				if (d < y0) y0 = d;
				if (d > y1) y1 = d;
				d = port.pos.z;
				if (d < z0) z0 = d;
				if (d > z1) z1 = d;
			}
		return new AxisAlignedBB(x0 - 0.03125, y0 - 0.03125, z0 - 0.03125, x1 + 0.03125, y1 + 0.03125, z1 + 0.03125).offset(pos);
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState blockState, World world, BlockPos pos, Vec3d start, Vec3d end) {
		RayTraceResult rtr = null;
		IBlockState cover = getCover(world, pos);
		if (cover != null) rtr = cover.collisionRayTrace(world, pos, start, end);
		return IHookAttachable.addBlockRayTrace(rtr, world, pos, start, end);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
		return super.getCollisionBoundingBox(blockState, worldIn, pos);
	}

}
