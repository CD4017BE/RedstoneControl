package cd4017be.rs_ctr;

import cd4017be.lib.render.SpecialModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 *
 * @author CD4017BE
 */
public class ClientProxy extends CommonProxy {

	@Override
	public void init() {
		super.init();
		//GUIs
	}

	@Override
	public void registerRenderers() {
		//TESR
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent ev) {
		SpecialModelLoader.setMod(Main.ID);
		//Models
		
		//item render reg
	}

}
