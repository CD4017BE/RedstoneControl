package cd4017be.rs_ctr;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.api.rs_ctr.sensor.SensorRegistry;
import cd4017be.api.rs_ctr.wire.RelayPort;

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
import cd4017be.rs_ctr.circuit.data.ArrayEditor;
import cd4017be.rs_ctr.circuit.data.GateConfiguration;
import cd4017be.rs_ctr.circuit.data.IntegerValue;
import cd4017be.rs_ctr.circuit.data.ToggleFlag;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.circuit.editor.IGateProvider;
import cd4017be.rs_ctr.circuit.gates.*;

import static cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet.registerTab;
import cd4017be.rs_ctr.item.ItemBlockProbe;
import cd4017be.rs_ctr.item.ItemWireCon;
import cd4017be.rs_ctr.port.BlockProbe;
import cd4017be.rs_ctr.port.Clock;
import cd4017be.rs_ctr.port.Constant;
import cd4017be.rs_ctr.port.EdgeTrigger;
import cd4017be.rs_ctr.port.PulseGen;
import cd4017be.rs_ctr.port.SplitPlug;
import cd4017be.rs_ctr.port.StatusLamp;
import cd4017be.rs_ctr.port.WireAnchor;
import cd4017be.rs_ctr.port.WireType;
import cd4017be.rs_ctr.sensor.BlockHardnessSensor;
import cd4017be.rs_ctr.sensor.DraconicFusionSensor;
import cd4017be.rs_ctr.sensor.FluidSensor;
import cd4017be.rs_ctr.sensor.ForgeEnergySensor;
import cd4017be.rs_ctr.sensor.GrowthSensor;
import cd4017be.rs_ctr.sensor.IC2EnergySensor;
import cd4017be.rs_ctr.sensor.ItemSensor;
import cd4017be.rs_ctr.sensor.LightSensor;
import cd4017be.rs_ctr.tileentity.BlockBreaker;
import cd4017be.rs_ctr.tileentity.BlockSelector;
import cd4017be.rs_ctr.tileentity.ChunkLoader;
import cd4017be.rs_ctr.tileentity.FluidTranslocator;
import cd4017be.rs_ctr.tileentity.ItemPlacer;
import cd4017be.rs_ctr.tileentity.ItemTranslocator;
import cd4017be.rs_ctr.tileentity.OC_Adapter;
import cd4017be.rs_ctr.tileentity.Panel;
import cd4017be.rs_ctr.tileentity.PowerHub;
import cd4017be.rs_ctr.tileentity.SolarCell;
import cd4017be.rs_ctr.tileentity.part.*;
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
		RelayPort.IMPLEMENTATION = WireAnchor::new;
		MinecraftForge.EVENT_BUS.register(this);
		registerGates();
		RecipeScriptContext.instance.modules.get("redstoneControl").assign("gate_cost", CircuitInstructionSet.INS_SET);
		RecipeAPI.Handlers.put(CIRCUIT_MAT, this);
		RecipeAPI.Handlers.put(BATTERY, this);
	}

	@SuppressWarnings("deprecation")
	public void init(ConfigConstants c) {
		ItemWireCon.MAX_LENGTH = (int)c.getNumber("max_wire_length", ItemWireCon.MAX_LENGTH);
		ItemBlockProbe.MAX_LENGTH = (int)c.getNumber("max_probe_lenght", ItemBlockProbe.MAX_LENGTH);
		SplitPlug.MAX_LINK_COUNT = (int)c.getNumber("max_split_links", SplitPlug.MAX_LINK_COUNT);
		BlockReference.INIT_LIFESPAN = (int)c.getNumber("blockref_livespan", BlockReference.INIT_LIFESPAN);
		double d;
		d = c.getNumber("panel_sync_dst_min", Math.sqrt(Panel.UPDATE_RANGE0)); Panel.UPDATE_RANGE0 = d * d;
		d = c.getNumber("panel_sync_dst_max", Math.sqrt(Panel.UPDATE_RANGE1)); Panel.UPDATE_RANGE1 = d * d;
		d = c.getNumber("panel_text_render_dst", Math.sqrt(Panel.TEXT_RANGE)); Panel.TEXT_RANGE = d * d;
		PowerHub.FE_UNIT = (long)c.getNumber("energy_unit_FE", PowerHub.FE_UNIT);
		OC_Adapter.OC_UNIT = c.getNumber("energy_unit_OC", OC_Adapter.OC_UNIT);
		ItemTranslocator.BASE_COST = -(int)c.getNumber("energy_item_translocator_op", -ItemTranslocator.BASE_COST);
		ItemTranslocator.TRANSFER_COST = -(int)c.getNumber("energy_item_translocator_ps", -ItemTranslocator.TRANSFER_COST);
		FluidTranslocator.BASE_COST = -(int)c.getNumber("energy_fluid_translocator_op", -FluidTranslocator.BASE_COST);
		FluidTranslocator.TRANSFER_COST = -(int)c.getNumber("energy_fluid_translocator_pb", -FluidTranslocator.TRANSFER_COST);
		FluidTranslocator.BLOCK_COST = -(int)c.getNumber("energy_fluid_translocator_world", -FluidTranslocator.BLOCK_COST);
		SolarCell.POWER = (int)c.getNumber("energy_solar", SolarCell.POWER);
		BlockSelector.RANGE = (int)c.getNumber("block_select_range", BlockSelector.RANGE);
		BlockBreaker.BASE_ENERGY = (float)c.getNumber("energy_breaker_op", BlockBreaker.BASE_ENERGY);
		BlockBreaker.ENERGY_MULT = (float)c.getNumber("energy_breaker_hard", BlockBreaker.ENERGY_MULT);
		BlockBreaker.SPEED_MOD = (float)c.getNumber("energy_breaker_speed", BlockBreaker.SPEED_MOD);
		BlockBreaker.NO_TOOL_MULT = (float)c.getNumber("energy_breaker_byhand", BlockBreaker.NO_TOOL_MULT);
		ItemPlacer.BASE_ENERGY = (float)c.getNumber("energy_placer_op", ItemPlacer.BASE_ENERGY);
		ItemPlacer.SPEED_MOD = (float)c.getNumber("energy_placer_speed", ItemPlacer.SPEED_MOD);
		ChunkLoader.RANGE = (int)c.getNumber("chunk_loader_range", ChunkLoader.RANGE);
		ChunkLoader.MAX_MINUTES = (int)(c.getNumber("chunkload_time_cap", ChunkLoader.MAX_MINUTES / 60D) * 60D);
		Objects.cl_fuel.setMaxDamage((int)(c.getNumber("chunkload_item_time", Objects.cl_fuel.getMaxDamage() / 60D) * 60D));
		
		registerSensor(new ItemSensor(), c.get("sensors_item", Object[].class, null));
		registerSensor(new FluidSensor(), c.get("sensors_fluid", Object[].class, null));
		registerSensor(new ForgeEnergySensor(), c.get("sensors_FE", Object[].class, null));
		registerSensor(new BlockHardnessSensor(), c.get("sensors_hard", Object[].class, null));
		registerSensor(new LightSensor(), c.get("sensors_light", Object[].class, null));
		registerSensor(new GrowthSensor(), c.get("sensors_grow", Object[].class, null));
		if (HAS_IC2_API)
			registerSensor(new IC2EnergySensor(), c.get("sensors_EU", Object[].class, null));
		if (net.minecraftforge.fml.common.Loader.isModLoaded("draconicevolution"))
			registerSensor((stack)-> new DraconicFusionSensor(), c.get("sensors_draconic", Object[].class, null));
		
		WireType.registerAll();
		IConnector.REGISTRY.put(Constant.ID, Constant::new);
		IConnector.REGISTRY.put(StatusLamp.ID, StatusLamp::new);
		IConnector.REGISTRY.put(BlockProbe.ID, BlockProbe::new);
		IConnector.REGISTRY.put(Clock.ID, Clock::new);
		IConnector.REGISTRY.put(EdgeTrigger.ID, EdgeTrigger::new);
		IConnector.REGISTRY.put(PulseGen.ID, PulseGen::new);
		
		Module.REGISTRY.put(_7Segment.ID, _7Segment::new);
		Module.REGISTRY.put(PointerDisplay.ID, PointerDisplay::new);
		Module.REGISTRY.put(Slider.ID, Slider::new);
		Module.REGISTRY.put(Text.ID, Text::new);
		Module.REGISTRY.put(Lever.ID, Lever::new);
		Module.REGISTRY.put(Lamp.ID, Lamp::new);
		Module.REGISTRY.put(Clock.ID, cd4017be.rs_ctr.tileentity.part.Clock::new);
		Module.REGISTRY.put(Trigger.ID, Trigger::new);
		Module.REGISTRY.put(Scale.ID, Scale::new);
		Module.REGISTRY.put(Offset.ID, Offset::new);
		Module.REGISTRY.put(Oscilloscope.ID, Oscilloscope::new);
		
		CircuitInstructionSet.INS_SET.loadTabs();
	}

	private void registerGates() {
		IGateProvider.REGISTRY.put("in", Input::new);
		IGateProvider.REGISTRY.put("out", Output::new);
		IGateProvider.REGISTRY.put("read", ReadVar::new);
		IGateProvider.REGISTRY.put("write", WriteVar::new);
		IGateProvider.REGISTRY.put("readwrite", ReadWriteVar::new);
		IGateProvider.REGISTRY.put("const", ConstNum::new);
		IGateProvider.REGISTRY.put("end", End::new);
		IGateProvider.REGISTRY.put("array", Array::new);
		GateConfiguration.REGISTRY.put("value", IntegerValue.VALUE);
		GateConfiguration.REGISTRY.put("interrupt", ToggleFlag.INTERRUPT);
		GateConfiguration.REGISTRY.put("sign", ToggleFlag.SIGNED);
		GateConfiguration.REGISTRY.put("arrayB", ArrayEditor.BYTE_ARRAY);
		GateConfiguration.REGISTRY.put("arrayS", ArrayEditor.SHORT_ARRAY);
		GateConfiguration.REGISTRY.put("arrayI", ArrayEditor.INT_ARRAY);
		GateConfiguration.REGISTRY.put("arrayF", ArrayEditor.FLOAT_ARRAY);
		
		registerTab("rs_ctr:io");
		registerTab("rs_ctr:logic");
		registerTab("rs_ctr:comp");
		registerTab("rs_ctr:num");
		registerTab("rs_ctr:bin");
	}

	private void registerSensor(IBlockSensor sensor, Object[] items) {
		registerSensor((stack)-> sensor, items);
	}

	private void registerSensor(Function<ItemStack, IBlockSensor> loader, Object[] items) {
		if (items == null) return;
		for (Object o : items)
			if (o instanceof ItemStack)
				SensorRegistry.register(loader, (ItemStack)o);
			else if (o instanceof String)
				SensorRegistry.register(loader, OreDictionary.getOres((String)o).toArray(new ItemStack[0]));
	}

}
