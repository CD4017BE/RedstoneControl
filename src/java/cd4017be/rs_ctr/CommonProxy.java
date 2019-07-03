package cd4017be.rs_ctr;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;

import java.util.HashMap;
import java.util.function.Function;

import org.apache.logging.log4j.core.util.Loader;

import cd4017be.api.recipes.ItemOperand;
import cd4017be.api.recipes.RecipeAPI;
import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.util.ItemKey;
import cd4017be.lib.util.OreDictStack;
import cd4017be.rs_ctr.api.sensor.IBlockSensor;
import cd4017be.rs_ctr.api.sensor.SensorRegistry;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.item.ItemBlockProbe;
import cd4017be.rs_ctr.item.ItemWireCon;
import cd4017be.rs_ctr.sensor.FluidSensor;
import cd4017be.rs_ctr.sensor.ForgeEnergySensor;
import cd4017be.rs_ctr.sensor.IC2EnergySensor;
import cd4017be.rs_ctr.sensor.ItemSensor;
import cd4017be.rs_ctr.signal.BlockProbe;
import cd4017be.rs_ctr.signal.Clock;
import cd4017be.rs_ctr.signal.Constant;
import cd4017be.rs_ctr.signal.StatusLamp;
import cd4017be.rs_ctr.signal.WireType;
import cd4017be.rs_ctr.tileentity.PowerHub;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy implements IRecipeHandler {

	public static final boolean HAS_IC2_API = Loader.isClassAvailable("ic2.api.tile.IEnergyStorage");
	public static final String CIRCUIT_MAT = "circuitMat", BATTERY = "battery";
	public static final int[] NULL = new int[6];
	public static final HashMap<ItemKey, int[]> MATERIALS = new HashMap<>();
	public static final HashMap<ItemKey, Long> BATTERIES = new HashMap<>();

	/**
	 * @param stack processor ingredient
	 * @return {basicCmplx, advCmplx, memory, size, gain, cap}
	 */
	public static int[] getStats(ItemStack stack) {
		int[] e = MATERIALS.get(new ItemKey(stack));
		return e == null ? NULL : e;
	}

	/**
	 * @param stack battery item for power hub
	 * @return capacity in %RF
	 */
	public static long getCap(ItemStack stack) {
		Long c = BATTERIES.get(new ItemKey(stack));
		return c == null ? 0 : c * (long)stack.getCount();
	}

	@Override
	public void addRecipe(Parameters param) {
		String key = param.getString(0);
		if (key.equals(CIRCUIT_MAT)) {
			double[] arr = param.getVectorOrAll(2);
			int[] stats = new int[NULL.length];
			int n = Math.min(arr.length, stats.length);
			for (int i = 0; i < n; i++)
				stats[i] = (int)arr[i];
			IOperand op = param.param[1];
			if (op instanceof ItemOperand)
				MATERIALS.put(new ItemKey(((ItemOperand)op).stack), stats);
			else if (op instanceof OreDictStack)
				for (ItemStack stack : ((OreDictStack)op).getItems())
					MATERIALS.put(new ItemKey(stack), stats);
		} else if (key.equals(BATTERY)) {
			long cap = (long)param.getNumber(2);
			IOperand op = param.param[1];
			if (op instanceof ItemOperand)
				BATTERIES.put(new ItemKey(((ItemOperand)op).stack), cap);
			else if (op instanceof OreDictStack)
				for (ItemStack stack : ((OreDictStack)op).getItems())
					BATTERIES.put(new ItemKey(stack), cap);
		}
	}

	public void preInit() {
		MinecraftForge.EVENT_BUS.register(this);
		RecipeScriptContext.instance.modules.get("redstoneControl").assign("gate_cost", CircuitInstructionSet.INS_SET);
		RecipeAPI.Handlers.put(CIRCUIT_MAT, this);
		RecipeAPI.Handlers.put(BATTERY, this);
	}

	public void init(ConfigConstants c) {
		ItemWireCon.MAX_LENGTH = (int)c.getNumber("max_wire_length", ItemWireCon.MAX_LENGTH);
		ItemBlockProbe.MAX_LENGTH = (int)c.getNumber("max_probe_lenght", ItemBlockProbe.MAX_LENGTH);
		PowerHub.FE_UNIT = (long)c.getNumber("energy_unit_FE", PowerHub.FE_UNIT);
		
		registerSensor(new ItemSensor(), c.get("sensors_item", Object[].class, null));
		registerSensor(new FluidSensor(), c.get("sensors_fluid", Object[].class, null));
		registerSensor(new ForgeEnergySensor(), c.get("sensors_FE", Object[].class, null));
		if (HAS_IC2_API)
			registerSensor(new IC2EnergySensor(), c.get("sensors_EU", Object[].class, null));
		
		WireType.registerAll();
		IConnector.REGISTRY.put(Constant.ID, Constant::new);
		IConnector.REGISTRY.put(StatusLamp.ID, StatusLamp::new);
		IConnector.REGISTRY.put(BlockProbe.ID, BlockProbe::new);
		IConnector.REGISTRY.put(Clock.ID, Clock::new);
	}

	private void registerSensor(IBlockSensor sensor, Object[] items) {
		if (items == null) return;
		Function<ItemStack, IBlockSensor> loader = (stack)-> sensor;
		for (Object o : items)
			if (o instanceof ItemStack)
				SensorRegistry.register(loader, (ItemStack)o);
			else if (o instanceof String)
				SensorRegistry.register(loader, OreDictionary.getOres((String)o).toArray(new ItemStack[0]));
	}

}
