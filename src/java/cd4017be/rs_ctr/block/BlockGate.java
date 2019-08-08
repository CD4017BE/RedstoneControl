package cd4017be.rs_ctr.block;

import cd4017be.api.rs_ctr.wire.IHookAttachable;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.property.PropertyOrientation;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


/**
 * @author CD4017BE
 *
 */
public class BlockGate extends OrientedBlock {

	/**
	 * @param id
	 * @param m
	 * @param sound
	 * @param flags
	 * @param tile
	 * @param prop
	 */
	public BlockGate(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile, PropertyOrientation prop) {
		super(id, m, sound, flags, tile, prop);
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState blockState, World world, BlockPos pos, Vec3d start, Vec3d end) {
		return IHookAttachable.addBlockRayTrace(super.collisionRayTrace(blockState, world, pos, start, end), world, pos, start, end);
	}

}
