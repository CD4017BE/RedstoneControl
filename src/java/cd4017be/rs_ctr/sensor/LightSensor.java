package cd4017be.rs_ctr.sensor;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;

/** @author CD4017BE */
public class LightSensor implements IBlockSensor {

	public static final ResourceLocation MODEL = new ResourceLocation(Main.ID, "block/_sensor.light()");

	@Override
	public int readValue(BlockReference block) {
		Chunk chunk = block.world().getChunkFromBlockCoords(block.pos);
		return chunk.getLightFor(EnumSkyBlock.BLOCK, block.pos)
			| chunk.getLightFor(EnumSkyBlock.SKY, block.pos) << 8;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.light");
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

}
