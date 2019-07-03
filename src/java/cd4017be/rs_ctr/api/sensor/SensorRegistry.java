package cd4017be.rs_ctr.api.sensor;

import java.util.HashMap;
import java.util.function.Function;

import cd4017be.lib.util.ItemKey;
import cd4017be.rs_ctr.api.interact.InteractiveDeviceRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * All sensor augments for the remote comparator are registered here.
 * @author CD4017BE
 */
public class SensorRegistry {

	/**all registered sensors */
	public static final HashMap<ItemKey, Function<ItemStack, IBlockSensor>> REGISTRY = new HashMap<>();

	/**
	 * register the given sensor with some Items.
	 * @param loader a function that loads a sensor implementation based of a given ItemStack (may depend on its NBT for more advanced logic).
	 * @param stacks list of Items that produce the given sensor when applied to a Remote Comparator (NBT data and stack-size are ignored).
	 */
	public static void register(Function<ItemStack, IBlockSensor> loader, ItemStack... stacks) {
		for (ItemStack stack : stacks)
			if (!stack.isEmpty())
				REGISTRY.put(new ItemKey(stack), loader);
	}

	/**
	 * @param stack an Item to be applied to a Remote Comparator
	 * @return the sensor associated with the given ItemStack or null if the Item doesn't represent a sensor
	 */
	public static IBlockSensor get(ItemStack stack) {
		Function<ItemStack, IBlockSensor> loader = REGISTRY.get(new ItemKey(stack));
		return loader == null ? DEFAULT : loader.apply(stack);
	}

	/**the default sensor behavior if no augment is installed */
	public static final IBlockSensor DEFAULT = new Comparator();

	/**
	 * the renderer that draws the models from {@link IBlockSensor#getModel()}<br>
	 * Add your ResourceLocations to {@link InteractiveDeviceRenderer#dependencies} during {@link ModelRegistryEvent}.
	 */
	@SideOnly(Side.CLIENT)
	public static InteractiveDeviceRenderer RENDERER;

}
