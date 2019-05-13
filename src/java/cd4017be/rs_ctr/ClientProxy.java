package cd4017be.rs_ctr;

import cd4017be.lib.render.model.MultipartModel;
import static cd4017be.rs_ctr.render.PortRenderer.PORT_RENDER;
import cd4017be.rs_ctr.tileentity.Gate;
import net.minecraftforge.client.event.ModelRegistryEvent;
import static net.minecraftforge.fml.client.registry.ClientRegistry.*;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import static cd4017be.rs_ctr.Objects.*;
import static cd4017be.lib.BlockItemRegistry.*;
import static cd4017be.lib.render.SpecialModelLoader.*;

/**
 *
 * @author CD4017BE
 */
public class ClientProxy extends CommonProxy {

	@Override
	public void init() {
		super.init();
		bindTileEntitySpecialRenderer(Gate.class, PORT_RENDER);
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent ev) {
		setMod(Main.ID);
		registerBlockModel(RS_PORT, new MultipartModel(RS_PORT).setPipeVariants(4).setProvider(7, PORT_RENDER));
		registerBlockModel(SPLITTER, new MultipartModel(SPLITTER, SPLITTER.orientProp, false, PORT_RENDER));
		registerBlockModel(ANALOG_COMB, new MultipartModel(ANALOG_COMB, ANALOG_COMB.orientProp, false, PORT_RENDER));
		registerBlockModel(LOGIC_COMB, new MultipartModel(LOGIC_COMB, LOGIC_COMB.orientProp, false, PORT_RENDER));
		registerBlockModel(NUM_COMB, new MultipartModel(NUM_COMB, NUM_COMB.orientProp, false, PORT_RENDER));
		
		PORT_RENDER.register("plug.logic(0)", "plug.logic(1)", "plug.logic(2)", "plug.logic(3)");
		PORT_RENDER.register("plug.main(0)", "plug.main(1)", "plug.main(2)", "plug.main(3)", "plug.main(4)");
		
		registerRenderBS(RS_PORT, 0, 1);
		registerRender(SPLITTER);
		registerRender(ANALOG_COMB);
		registerRender(LOGIC_COMB);
		registerRender(NUM_COMB);
		registerRender(wire);
		registerRender(wireless);
		registerRender(constant);
		registerRender(lamp);
		registerRender(tag);
	}

}
