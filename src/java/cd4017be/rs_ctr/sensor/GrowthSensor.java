package cd4017be.rs_ctr.sensor;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockReed;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.IPlantable;

/** @author CD4017BE */
public class GrowthSensor implements IBlockSensor {

	public static final ResourceLocation MODEL = new ResourceLocation(
		Main.ID, "block/_sensor.grow()"
	);

	@Override
	public int readValue(BlockReference block) {
		IBlockState state = block.getState();
		Block b = state.getBlock();
		if(b instanceof IGrowable)
			return ((IGrowable)b).canGrow(
				block.world(), block.pos, state, false
			) ? 0 : 1;
		if(b instanceof IPlantable)
			return isMatureHardcoded(b, state, block) ? 1 : 0;
		return -1;
	}

	private static boolean isMatureHardcoded(
		Block block, IBlockState state, BlockReference ref
	) {
		if(block instanceof BlockNetherWart)
			return state.getValue(BlockNetherWart.AGE) >= 3;
		if(block instanceof BlockCactus || block instanceof BlockReed)
			return ref.world().getBlockState(ref.pos.down()).getBlock() == block;
		return true;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.grow");
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

}
