package cd4017be.rs_ctr;

import java.io.File;
import java.util.List;
import org.apache.logging.log4j.Logger;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.api.rs_ctr.port.Link;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.rs_ctr.tileentity.OC_Adapter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

/**
 * @author CD4017BE
 *
 */
@Mod(modid = Main.ID, useMetadata = true)
public class Main implements LoadingCallback {

	public static final String ID = "rs_ctr";

	@Instance(ID)
	public static Main instance;

	public static Logger LOG;

	@SidedProxy(clientSide="cd4017be." + ID + ".ClientProxy", serverSide="cd4017be." + ID + ".CommonProxy")
	public static CommonProxy proxy;

	public Main() {
		RecipeScriptContext.scriptRegistry.add(new Version("redstoneControl", "/assets/" + ID + "/config/recipes.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOG = event.getModLog();
		ForgeChunkManager.setForcedChunkLoadingCallback(this, this);
		proxy.preInit();
		RecipeScriptContext.instance.run("redstoneControl.PRE_INIT");
	}

	@Mod.EventHandler
	public void load(FMLInitializationEvent event) {
		Objects.init();
		proxy.init(new ConfigConstants(RecipeScriptContext.instance.modules.get("redstoneControl")));
		if (Loader.isModLoaded("opencomputers"))
			OC_Adapter.registerAPI();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@Mod.EventHandler
	public void serverStart(FMLServerAboutToStartEvent event) {
		Link.loadData(new File(FMLCommonHandler.instance().getSavesDirectory(), event.getServer().getFolderName()));
	}

	@Mod.EventHandler
	public void serverStop(FMLServerStoppedEvent event) {
		Link.saveData();
	}

	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world) {
		for(Ticket t : tickets) {
			NBTTagCompound nbt = t.getModData();
			if(t.getType() == Type.NORMAL) {
				BlockPos pos = BlockPos.fromLong(nbt.getLong("pos"));
				TileEntity te = world.getTileEntity(pos);
				if(te instanceof IChunkLoader && ((IChunkLoader)te).setTicket(t)) continue;
			}
			ForgeChunkManager.releaseTicket(t);
		}
	}

}
