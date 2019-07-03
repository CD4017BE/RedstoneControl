package cd4017be.rs_ctr.api.sensor;

import javax.annotation.Nullable;

import cd4017be.rs_ctr.api.com.BlockReference;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Remote Comparator API for special sensor augments.<dl>
 * To add your own sensors simply write an implementation for this interface and register an instance of it alongside one or more ItemStacks using {@link SensorRegistry#register(java.util.function.Function, ItemStack...)}.
 * @author CD4017BE
 */
public interface IBlockSensor {

	/**
	 * perform the scan operation on a given BlockReference
	 * @param block the block face to scan (chunk is guaranteed to be loaded).
	 * @return a signal value for the given block
	 */
	int readValue(BlockReference block);

	/**
	 * @return text displayed when aimed at
	 */
	String getTooltipString();

	/**
	 * @return a model to render on top of the remote comparator
	 * @see SensorRegistry#RENDERER
	 */
	@Nullable ResourceLocation getModel();

}
