package cd4017be.rs_ctr;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import cd4017be.lib.block.AdvancedBlock;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.templates.BaseSound;
import cd4017be.lib.templates.TabMaterials;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.block.*;
import cd4017be.rs_ctr.item.*;
import cd4017be.rs_ctr.port.BlockSplitPlug;
import cd4017be.rs_ctr.port.SignalSplitPlug;
import cd4017be.rs_ctr.port.WireType;
import cd4017be.rs_ctr.tileentity.*;
import static cd4017be.rs_ctr.block.PropertyGateOrient.GATE_ORIENT;
import static cd4017be.lib.property.PropertyOrientation.*;

/**
 * 
 * @author CD4017BE
 */
@EventBusSubscriber(modid = Main.ID)
@ObjectHolder(value = Main.ID)
public class Objects {

	//Creative Tabs
	public static TabMaterials tabCircuits = new TabMaterials(Main.ID);

	//Blocks
	public static final BlockRedstonePort RS_PORT = null;
	public static final BlockGate SPLITTER = null;
	public static final BlockGate ANALOG_COMB = null;
	public static final BlockGate LOGIC_COMB = null;
	public static final BlockGate NUM_COMB = null;
	public static final BlockGate BIN_COMB = null;
	public static final BlockGate BIN_SPLIT = null;
	public static final BlockGate XOR_GATE = null;
	public static final BlockGate COUNTER = null;
	public static final BlockGate DELAY = null;
	public static final BlockGate SPLITTER_B = null;
	public static final BlockGate MULTIPLEX_B = null;
	public static final BlockGate DELAY_B = null;
	public static final BlockGate PROCESSOR = null;
	public static final BlockGate PROCESSOR2 = null;
	public static final BlockGate PROCESSOR3 = null;
	public static final AdvancedBlock EDITOR = null;
	public static final AdvancedBlock ASSEMBLER = null;
	public static final BlockWireAnchor WIRE_ANCHOR = null;
	public static final BlockGate COMPARATOR = null;
	public static final BlockGate POWER_HUB = null;
	public static final BlockGate ITEM_TRANSLOCATOR = null;
	public static final BlockGate FLUID_TRANSLOCATOR = null;
	public static final BlockGate ENERGY_VALVE = null;
	public static final BlockGate PANEL = null;
	public static final BlockGate SOLAR_CELL = null;
	public static final BlockGate BLOCK_BREAKER = null;
	public static final BlockGate BLOCK_BREAKER1 = null;
	public static final BlockGate ITEM_PLACER = null;
	public static final AdvancedBlock FRAME = null;
	public static final BlockGate BLOCK_SELECTOR = null;
	public static final BlockGate OC_ADAPTER = null;
	public static final BlockGate RAM = null;
	public static final BlockGate CHUNK_LOADER = null;
	public static final BlockGate TELEPORTER = null;
	public static final BlockGate TELEPORTER1 = null;
	public static final BlockGate BLOCK_DESERIALIZER = null;
	public static final BlockGate BLOCK_REORIENTER = null;
	public static final BlockGate INT_PANEL = null;
	
	//ItemBlocks
	public static final ItemRedstonePort rs_port = null;
	public static final BaseItemBlock splitter = null;
	public static final BaseItemBlock analog_comb = null;
	public static final BaseItemBlock logic_comb = null;
	public static final BaseItemBlock num_comb = null;
	public static final BaseItemBlock bin_comb = null;
	public static final BaseItemBlock bin_split = null;
	public static final BaseItemBlock xor_gate = null;
	public static final BaseItemBlock counter = null;
	public static final BaseItemBlock delay = null;
	public static final BaseItemBlock splitter_b = null;
	public static final BaseItemBlock multiplex_b = null;
	public static final BaseItemBlock delay_b = null;
	public static final ItemProcessor processor = null;
	public static final ItemProcessor processor2 = null;
	public static final ItemProcessor processor3 = null;
	public static final BaseItemBlock editor = null;
	public static final BaseItemBlock assembler = null;
	public static final ItemWireAnchor wire_anchor = null;
	public static final BaseItemBlock comparator = null;
	public static final BaseItemBlock power_hub = null;
	public static final BaseItemBlock item_translocator = null;
	public static final BaseItemBlock fluid_translocator = null;
	public static final BaseItemBlock energy_valve = null;
	public static final BaseItemBlock panel = null;
	public static final BaseItemBlock solar_cell = null;
	public static final ItemBlockBreaker block_breaker = null;
	public static final BaseItemBlock item_placer = null;
	public static final BaseItemBlock frame = null;
	public static final BaseItemBlock block_selector = null;
	public static final BaseItemBlock oc_adapter = null;
	public static final BaseItemBlock ram = null;
	public static final BaseItemBlock chunk_loader = null;
	public static final BaseItemBlock teleporter = null;
	public static final BaseItemBlock teleporter1 = null;
	public static final BaseItemBlock block_deserializer = null;
	public static final BaseItemBlock block_reorienter = null;
	public static final ItemProcessor int_panel = null;

	//Items
	public static final ItemWireCon wire = null, wire_e = null, block_wire = null;
	public static final ItemSplitCon split_s = null;
	public static final ItemSplitCon split_b = null;
	public static final ItemWirelessCon wireless = null, wireless_b = null;
	public static final ItemConstantPlug constant = null;
	public static final ItemStatusLamp lamp = null;
	public static final ItemWireTag tag = null;
	public static final ItemClock clock = null;
	public static final ItemEdgeTrigger edge_trigger = null;
	public static final ItemPulseGen pulse_gen = null;
	public static final BaseItem circuitboard = null;
	public static final ItemPanelModule seg7 = null;
	public static final ItemPanelModule pointer_dsp = null;
	public static final ItemPanelModule slider = null;
	public static final ItemPanelModule text = null;
	public static final ItemPanelModule lever = null;
	public static final ItemPanelModule trigger = null;
	public static final ItemPanelModule scale = null;
	public static final ItemPanelModule offset = null;
	public static final ItemPanelModule oscilloscope = null;
	public static final ItemWrench wrench = null;
	public static final ItemChunkLoaderFuel cl_fuel = null;
	public static final ItemSignalProbe signal_probe = null;
	public static final ItemBlockProbe block_plug = null;
	public static final ItemRemoteControl remote = null;
	public static final ItemPanelModule socket_s = null;
	public static final ItemPanelModule socket_b = null;

	//Sounds
	public static final BaseSound LEVER_FLIP = null;
	public static final BaseSound BUTTON_DOWN = null;
	public static final BaseSound BUTTON_UP = null;

	public static void init() {
		tabCircuits.item = new ItemStack(wire);
	}

	private static final AxisAlignedBB GATE_FORM_FACTOR = new AxisAlignedBB(0.25, 0, 0, 0.75, 1, 0.25);

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
				new BlockRedstonePort("rs_port", Material.ROCK, SoundType.STONE, RedstonePort.class).setCreativeTab(tabCircuits).setLightOpacity(0).setHardness(0.5F),
				new BlockGate("splitter", Material.ROCK, SoundType.STONE, 3, SignalSplitter.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("analog_comb", Material.ROCK, SoundType.STONE, 3, AnalogCombiner.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("logic_comb", Material.ROCK, SoundType.STONE, 3, LogicCombiner.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("num_comb", Material.ROCK, SoundType.STONE, 3, NummericCombiner.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("bin_comb", Material.ROCK, SoundType.STONE, 3, BinaryCombiner.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("bin_split", Material.ROCK, SoundType.STONE, 3, BinarySplitter.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("xor_gate", Material.ROCK, SoundType.STONE, 3, XORGate.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("counter", Material.ROCK, SoundType.STONE, 3, Counter.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("delay", Material.ROCK, SoundType.STONE, 3, SignalDelayer.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("splitter_b", Material.ROCK, SoundType.STONE, 3, BlockSplitter.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("multiplex_b", Material.ROCK, SoundType.STONE, 3, BlockMultiplexer.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("delay_b", Material.ROCK, SoundType.STONE, 3, BlockDelayer.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockWireAnchor("wire_anchor", Material.IRON, SoundType.METAL, 3, WireAnchor.class).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("processor", Material.ROCK, SoundType.STONE, 7, Processor.class, GATE_ORIENT).setBlockBounds(new AxisAlignedBB(0, 0, 0, 1, 1, 0.5)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("processor2", Material.ROCK, SoundType.STONE, 7, Processor.class, GATE_ORIENT).setBlockBounds(new AxisAlignedBB(0, 0, 0, 1, 1, 0.5)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("processor3", Material.ROCK, SoundType.STONE, 7, Processor.class, GATE_ORIENT).setBlockBounds(new AxisAlignedBB(0, 0, 0.5, 1, 1, 0.875)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new OrientedBlock("editor", Material.WOOD, SoundType.WOOD, 0, Editor.class, HOR_AXIS).setCreativeTab(tabCircuits),
				new OrientedBlock("assembler", Material.IRON, SoundType.ANVIL, 0, Assembler.class, HOR_AXIS).setCreativeTab(tabCircuits),
				new BlockGate("comparator", Material.ROCK, SoundType.STONE, 3, Sensor.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("power_hub", Material.ROCK, SoundType.STONE, 3, PowerHub.class, GATE_ORIENT).setBlockBounds(new AxisAlignedBB(0, 0, 0, 1, 1, 0.5)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("item_translocator", Material.ROCK, SoundType.STONE, 3, ItemTranslocator.class, HOR_AXIS).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("fluid_translocator", Material.ROCK, SoundType.STONE, 3, FluidTranslocator.class, HOR_AXIS).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("energy_valve", Material.ROCK, SoundType.STONE, 3, EnergyValve.class, GATE_ORIENT).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("panel", Material.ROCK, SoundType.STONE, 31, Panel.class, XY_12_ROT).setBlockBounds(new AxisAlignedBB(0, 0, 0.75, 1, 1, 1)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("solar_cell", Material.ROCK, SoundType.GLASS, 3, SolarCell.class, HOR_AXIS).setBlockBounds(new AxisAlignedBB(0, 0, 0, 1, .25, 1)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("block_breaker", Material.IRON, SoundType.STONE, 3, BlockBreaker.class, HOR_AXIS).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("block_breaker1", Material.IRON, SoundType.STONE, 3, BlockBreaker.class, HOR_AXIS).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("item_placer", Material.IRON, SoundType.STONE, 3, ItemPlacer.class, HOR_AXIS).setLightOpacity(0).setCreativeTab(tabCircuits),
				new AdvancedBlock("frame", Material.ROCK, SoundType.STONE, 0, BlockFrame.class).setHardness(2.5F).setCreativeTab(tabCircuits),
				new BlockGate("block_selector", Material.ROCK, SoundType.STONE, 0, BlockSelector.class, HOR_AXIS).setCreativeTab(tabCircuits),
				new BlockGate("oc_adapter", Material.ROCK, SoundType.STONE, 3, OC_Adapter.class, HOR_AXIS).setBlockBounds(new AxisAlignedBB(0, 0, 0, 1, 1, 0.75)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("ram", Material.ROCK, SoundType.STONE, 3, RAM.class, GATE_ORIENT).setBlockBounds(new AxisAlignedBB(0, 0, 0, 1, 1, 0.5)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("chunk_loader", Material.ROCK, SoundType.STONE, 0, ChunkLoader.class, HOR_AXIS).setCreativeTab(tabCircuits),
				new BlockGate("teleporter", Material.ROCK, SoundType.STONE, 0, StructTeleporter.class, HOR_AXIS).setCreativeTab(tabCircuits),
				new BlockGate("teleporter1", Material.ROCK, SoundType.STONE, 3, Teleporter.class, HOR_AXIS).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("block_deserializer", Material.ROCK, SoundType.STONE, 3, BlockDeserializer.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("block_reorienter", Material.ROCK, SoundType.STONE, 3, BlockReorienter.class, GATE_ORIENT).setBlockBounds(GATE_FORM_FACTOR).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("int_panel", Material.ROCK, SoundType.STONE, 31, IntegratedPanel.class, XY_12_ROT).setBlockBounds(new AxisAlignedBB(0, 0, 0.5, 1, 1, 1)).setLightOpacity(0).setCreativeTab(tabCircuits)
		);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
			//wiring stuff
			new ItemSignalProbe("signal_probe").setCreativeTab(tabCircuits),
			new ItemRedstonePort(RS_PORT),
			new ItemWireAnchor(WIRE_ANCHOR),
			WireType.SIGNAL.wireItem = new ItemWireCon("wire", WireType.SIGNAL).setCreativeTab(tabCircuits),
			WireType.BLOCK.wireItem = new ItemWireCon("block_wire", WireType.BLOCK).setCreativeTab(tabCircuits),
			new ItemSplitCon("split_s", WireType.SIGNAL, SignalSplitPlug::new).setCreativeTab(tabCircuits),
			new ItemSplitCon("split_b", WireType.BLOCK, BlockSplitPlug::new).setCreativeTab(tabCircuits),
			WireType.SIGNAL.wirelessItem = new ItemWirelessCon("wireless", WireType.SIGNAL).setCreativeTab(tabCircuits),
			WireType.BLOCK.wirelessItem = new ItemWirelessCon("wireless_b", WireType.BLOCK).setCreativeTab(tabCircuits),
			new ItemWireTag("tag").setCreativeTab(tabCircuits),
			new ItemStatusLamp("lamp").setCreativeTab(tabCircuits),
			new ItemConstantPlug("constant").setCreativeTab(tabCircuits),
			new ItemClock("clock").setCreativeTab(tabCircuits),
			new ItemBlockProbe("block_plug").setCreativeTab(tabCircuits),
			new ItemEdgeTrigger("edge_trigger").setCreativeTab(tabCircuits),
			new ItemPulseGen("pulse_gen").setCreativeTab(tabCircuits),
			new ItemRemoteControl("remote").setCreativeTab(tabCircuits),
			//basic signal & block-ref operators
			new BaseItemBlock(SPLITTER),
			new BaseItemBlock(DELAY),
			new BaseItemBlock(ANALOG_COMB),
			new BaseItemBlock(LOGIC_COMB),
			new BaseItemBlock(XOR_GATE),
			new BaseItemBlock(NUM_COMB),
			new BaseItemBlock(BIN_COMB),
			new BaseItemBlock(BIN_SPLIT),
			new BaseItemBlock(COUNTER),
			new BaseItemBlock(COMPARATOR),
			new BaseItemBlock(SPLITTER_B),
			new BaseItemBlock(DELAY_B),
			new BaseItemBlock(MULTIPLEX_B),
			new BaseItemBlock(BLOCK_REORIENTER),
			new BaseItemBlock(BLOCK_DESERIALIZER),
			//FPGA stuff
			new BaseItemBlock(ASSEMBLER),
			new BaseItemBlock(EDITOR),
			new ItemProcessor(PROCESSOR2, 4, 4),
			new ItemProcessor(PROCESSOR, 6, 6),
			new ItemProcessor(PROCESSOR3, 8, 8),
			new ItemRAM(RAM),
			//control panel
			new BaseItemBlock(PANEL),
			new ItemProcessor(INT_PANEL, 16, 16),
			new ItemWrench("wrench").setCreativeTab(tabCircuits),
			new ItemPanelModule("socket_s", "socket_s").setCreativeTab(tabCircuits),
			new ItemPanelModule("socket_b", "socket_b").setCreativeTab(tabCircuits),
			new ItemPanelModule("text", "text", true, 4, 1, 2, 3).setCreativeTab(tabCircuits),
			new ItemPanelModule("lever", "lever", false, 0, 1, 2).setCreativeTab(tabCircuits),
			new ItemPanelModule("slider", "slider").setCreativeTab(tabCircuits),
			new ItemPanelModule("pointer_dsp", "pointer", true, 0, 1, 2, 3).setCreativeTab(tabCircuits),
			new ItemPanelModule("seg7", "7seg", true, 0, 1, 2, 3).setCreativeTab(tabCircuits),
			new ItemPanelModule("oscilloscope", "oscilloscope", true, 0, 1, 2, 3).setCreativeTab(tabCircuits),
			new ItemPanelModule("offset", "offset").setCreativeTab(tabCircuits),
			new ItemPanelModule("scale", "scale").setCreativeTab(tabCircuits),
			new ItemPanelModule("trigger", "trigger").setCreativeTab(tabCircuits),
			//energy stuff
			new BaseItemBlock(ENERGY_VALVE),
			new BaseItemBlock(OC_ADAPTER),
			new BaseItemBlock(POWER_HUB),
			WireType.ENERGY.wireItem = new ItemWireCon("wire_e", WireType.ENERGY).setCreativeTab(tabCircuits),
			new BaseItemBlock(SOLAR_CELL),
			//machines
			new BaseItemBlock(ITEM_TRANSLOCATOR),
			new BaseItemBlock(FLUID_TRANSLOCATOR),
			new BaseItemBlock(BLOCK_BREAKER1),
			new ItemBlockBreaker(BLOCK_BREAKER),
			new BaseItemBlock(ITEM_PLACER),
			new BaseItemBlock(TELEPORTER1),
			//3D frame devices
			new BaseItemBlock(FRAME),
			new BaseItemBlock(BLOCK_SELECTOR),
			new BaseItemBlock(TELEPORTER),
			new BaseItemBlock(CHUNK_LOADER),
			new ItemChunkLoaderFuel("cl_fuel").setCreativeTab(tabCircuits)
		);
	}

	@SubscribeEvent
	public static void registerSounds(RegistryEvent.Register<SoundEvent> ev) {
		ev.getRegistry().registerAll(
				new BaseSound(new ResourceLocation(Main.ID, "lever_flip")),
				new BaseSound(new ResourceLocation(Main.ID, "button_down")),
				new BaseSound(new ResourceLocation(Main.ID, "button_up"))
		);
	}
}
