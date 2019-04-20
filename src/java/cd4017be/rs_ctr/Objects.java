package cd4017be.rs_ctr;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.lib.templates.TabMaterials;

/**
 * 
 * @author CD4017BE
 */
@EventBusSubscriber(modid = Main.ID)
@ObjectHolder(value = Main.ID)
public class Objects {

	//Capabilities

	//Creative Tabs
	public static TabMaterials tabCircuits = new TabMaterials(Main.ID);

	//Blocks

	//ItemBlocks

	//Items

	public static void init() {
		tabCircuits.item = new ItemStack(Blocks.REDSTONE_TORCH);
	}

	public static void initConstants(ConfigConstants c) {
		
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		ev.getRegistry().registerAll(
			
		);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> ev) {
		ev.getRegistry().registerAll(
			
		);
	}

}
