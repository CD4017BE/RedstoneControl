package cd4017be.rs_ctr.sensor;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import ic2.api.tile.IEnergyStorage;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;


/**
 * Reads the amount of Industrialcraft EU stored in a block.
 * @author CD4017BE
 */
public class IC2EnergySensor implements IBlockSensor {

	public static final ResourceLocation MODEL = new ResourceLocation(Main.ID, "block/_sensor.eu()");

	@Override
	public int readValue(BlockReference block) {
		TileEntity te = block.getTileEntity();
		return te instanceof IEnergyStorage ? ((IEnergyStorage)te).getStored() : 0;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.eu");
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

}
