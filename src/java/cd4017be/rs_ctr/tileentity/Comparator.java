package cd4017be.rs_ctr.tileentity;

import cd4017be.rs_ctr.api.com.BlockReference;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author CD4017BE
 *
 */
public class Comparator extends Sensor {

	@Override
	protected int readValue(BlockReference ref) {
		World world = blockRef.world; BlockPos pos = blockRef.pos;
		return world.getBlockState(pos).getComparatorInputOverride(world, pos);
	}

}
