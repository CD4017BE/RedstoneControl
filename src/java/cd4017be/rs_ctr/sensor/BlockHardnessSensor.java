package cd4017be.rs_ctr.sensor;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** @author CD4017BE */
public class BlockHardnessSensor implements IBlockSensor {

	public static final ResourceLocation MODEL = new ResourceLocation(Main.ID, "block/_sensor.hard()");
	private static final float REF = 100F / Blocks.STONE.getDefaultState()
	.getBlockHardness(null, BlockPos.ORIGIN);

	@Override
	public int readValue(BlockReference block) {
		World world = block.world();
		float h = world.getBlockState(block.pos)
		.getBlockHardness(world, block.pos);
		return h < 0 ? -1 : Math.round(h * REF);
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.hard");
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

}
