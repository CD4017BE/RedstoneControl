package cd4017be.rs_ctr;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.lib.block.AdvancedBlock;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.templates.TabMaterials;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.block.*;
import cd4017be.rs_ctr.item.*;
import cd4017be.rs_ctr.tileentity.*;

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
	public static final AdvancedBlock EDITOR = null;
	public static final BlockWireAnchor WIRE_ANCHOR = null;
	
	//ItemBlocks
	public static final ItemRedstonePort rs_port = null;
	public static final BaseItemBlock splitter = null;
	public static final BaseItemBlock analog_comb = null;
	public static final BaseItemBlock logic_comb = null;
	public static final BaseItemBlock num_comb = null;
	public static final BaseItemBlock bin_comb = null;
	public static final BaseItemBlock bin_split = null;
	public static final BaseItemBlock editor = null;
	public static final ItemWireAnchor wire_anchor = null;

	//Items
	public static final ItemSignalWire wire = null;
	public static final ItemWirelessCon wireless = null;
	public static final ItemConstantPlug constant = null;
	public static final ItemStatusLamp lamp = null;
	public static final ItemWireTag tag = null;
	public static final BaseItem circuitboard = null;

	public static void init() {
		tabCircuits.item = new ItemStack(wire);
	}

	public static void initConstants(ConfigConstants c) {
		ItemSignalWire.MAX_LENGTH = (int)c.getNumber("max_wire_length", ItemSignalWire.MAX_LENGTH);
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
				new BlockRedstonePort("rs_port", Material.ROCK, SoundType.STONE, RedstonePort.class).setCreativeTab(tabCircuits).setLightOpacity(0).setHardness(0.5F),
				new BlockGate("splitter", Material.ROCK, SoundType.STONE, 3, SignalSplitter.class).setBlockBounds(new AxisAlignedBB(0.25, 0, 0, 0.75, 1, 0.25)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("analog_comb", Material.ROCK, SoundType.STONE, 3, AnalogCombiner.class).setBlockBounds(new AxisAlignedBB(0.25, 0, 0, 0.75, 1, 0.25)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("logic_comb", Material.ROCK, SoundType.STONE, 3, LogicCombiner.class).setBlockBounds(new AxisAlignedBB(0.25, 0, 0, 0.75, 1, 0.25)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("num_comb", Material.ROCK, SoundType.STONE, 3, NummericCombiner.class).setBlockBounds(new AxisAlignedBB(0.25, 0, 0, 0.75, 1, 0.25)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("bin_comb", Material.ROCK, SoundType.STONE, 3, BinaryCombiner.class).setBlockBounds(new AxisAlignedBB(0.25, 0, 0, 0.75, 1, 0.25)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockGate("bin_split", Material.ROCK, SoundType.STONE, 3, BinarySplitter.class).setBlockBounds(new AxisAlignedBB(0.25, 0, 0, 0.75, 1, 0.25)).setLightOpacity(0).setCreativeTab(tabCircuits),
				new BlockWireAnchor("wire_anchor", Material.IRON, SoundType.METAL, 3, WireAnchor.class).setLightOpacity(0).setCreativeTab(tabCircuits),
				new AdvancedBlock("editor", Material.IRON, SoundType.METAL, 0, Editor.class).setCreativeTab(tabCircuits)
		);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
				new ItemRedstonePort(RS_PORT),
				new BaseItemBlock(SPLITTER),
				new BaseItemBlock(ANALOG_COMB),
				new BaseItemBlock(LOGIC_COMB),
				new BaseItemBlock(NUM_COMB),
				new BaseItemBlock(BIN_COMB),
				new BaseItemBlock(BIN_SPLIT),
				new ItemWireAnchor(WIRE_ANCHOR),
				new BaseItemBlock(EDITOR),
				new ItemSignalWire("wire").setCreativeTab(tabCircuits),
				new ItemWirelessCon("wireless").setCreativeTab(tabCircuits),
				new ItemConstantPlug("constant").setCreativeTab(tabCircuits),
				new ItemStatusLamp("lamp").setCreativeTab(tabCircuits),
				new ItemWireTag("tag").setCreativeTab(tabCircuits)
		);
	}

}
