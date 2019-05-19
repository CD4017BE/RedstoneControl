package cd4017be.rs_ctr;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.render.model.MultipartModel;
import cd4017be.rs_ctr.api.interact.InteractiveDeviceRenderer;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.gui.CircuitEditor;
import cd4017be.rs_ctr.render.WireRenderer;
import static cd4017be.rs_ctr.render.PortRenderer.PORT_RENDER;
import cd4017be.rs_ctr.tileentity.Gate;
import cd4017be.rscpl.gui.Category;
import cd4017be.rscpl.gui.GateTextureHandler;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import static net.minecraftforge.fml.client.registry.ClientRegistry.*;

import java.util.Collections;

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
		BlockGuiHandler.registerGui(EDITOR, CircuitEditor.class);
		
		bindTileEntitySpecialRenderer(Gate.class, PORT_RENDER);
		
		for (Category c : CircuitInstructionSet.TABS)
			GateTextureHandler.ins_sets.add(c);
		GateTextureHandler.register();
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent ev) {
		setMod(Main.ID);
		IStateMapper sm = new DefaultStateMapper();
		registerBlockModel(RS_PORT, new MultipartModel(RS_PORT).setPipeVariants(4).setProvider(7, PORT_RENDER));
		overrideBlockModel(SPLITTER, new MultipartModel(SPLITTER, sm.putStateModelLocations(SPLITTER), false, PORT_RENDER));
		overrideBlockModel(ANALOG_COMB, new MultipartModel(ANALOG_COMB, sm.putStateModelLocations(ANALOG_COMB), false, PORT_RENDER));
		overrideBlockModel(LOGIC_COMB, new MultipartModel(LOGIC_COMB, sm.putStateModelLocations(LOGIC_COMB), false, PORT_RENDER));
		overrideBlockModel(NUM_COMB, new MultipartModel(NUM_COMB, sm.putStateModelLocations(NUM_COMB), false, PORT_RENDER));
		overrideBlockModel(WIRE_ANCHOR, new MultipartModel(WIRE_ANCHOR, Collections.singletonMap(WIRE_ANCHOR.getDefaultState(), new ModelResourceLocation(WIRE_ANCHOR.getRegistryName(), "empty")), false, PORT_RENDER));
		
		SPLITTER.setBlockLayer(BlockRenderLayer.CUTOUT);
		ANALOG_COMB.setBlockLayer(BlockRenderLayer.CUTOUT);
		LOGIC_COMB.setBlockLayer(BlockRenderLayer.CUTOUT);
		NUM_COMB.setBlockLayer(BlockRenderLayer.CUTOUT);
		WIRE_ANCHOR.setBlockLayer(BlockRenderLayer.CUTOUT);
		
		PORT_RENDER.register("_plug.num(0)", "_plug.num(1)");
		PORT_RENDER.register("_plug.logic(0)", "_plug.logic(1)", "_plug.logic(2)", "_plug.logic(3)");
		PORT_RENDER.register("_plug.main(0)", "_plug.main(1)", "_plug.main(2)", "_plug.main(3)", "_plug.main(4)");
		PORT_RENDER.register("_hook.pin(1)", "_hook.pin(2)", "_hook.pin(3)");
		
		registerRenderBS(RS_PORT, 0, 1);
		registerRender(SPLITTER);
		registerRender(ANALOG_COMB);
		registerRender(LOGIC_COMB);
		registerRender(NUM_COMB);
		registerRender(WIRE_ANCHOR);
		registerRender(wire);
		registerRender(wireless);
		registerRender(constant);
		registerRender(lamp);
		registerRender(tag);
	}

}
