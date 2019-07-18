package cd4017be.rs_ctr;

import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.api.rs_ctr.sensor.SensorRegistry;
import cd4017be.lib.render.model.MultipartModel;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.render.PanelRenderer;
import cd4017be.rs_ctr.sensor.FluidSensor;
import cd4017be.rs_ctr.sensor.ForgeEnergySensor;
import cd4017be.rs_ctr.sensor.IC2EnergySensor;
import cd4017be.rs_ctr.sensor.ItemSensor;

import static cd4017be.rs_ctr.render.PortRenderer.PORT_RENDER;

import cd4017be.rs_ctr.block.BlockGate;
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
	public void preInit() {
		super.preInit();
		SensorRegistry.RENDERER = PORT_RENDER;
		PanelRenderer.register();
	}

	@Override
	public void init(ConfigConstants cc) {
		super.init(cc);
		bindTileEntitySpecialRenderer(Gate.class, PORT_RENDER);
		
		for (Category c : CircuitInstructionSet.TABS)
			GateTextureHandler.ins_sets.add(c);
		GateTextureHandler.register();
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent ev) {
		setMod(Main.ID);
		
		registerBlockModel(RS_PORT, new MultipartModel(RS_PORT).setPipeVariants(4).setProvider(7, PORT_RENDER));
		overrideBlockModel(WIRE_ANCHOR, new MultipartModel(WIRE_ANCHOR, Collections.singletonMap(WIRE_ANCHOR.getDefaultState(), new ModelResourceLocation(WIRE_ANCHOR.getRegistryName(), "empty")), false, PORT_RENDER));
		addGates(SPLITTER, ANALOG_COMB, LOGIC_COMB, NUM_COMB, BIN_COMB, BIN_SPLIT, XOR_GATE, PROCESSOR, COMPARATOR, POWER_HUB, ITEM_TRANSLOCATOR, ANALOG_DSP);
		
		WIRE_ANCHOR.setBlockLayer(BlockRenderLayer.CUTOUT);
		
		PORT_RENDER.register("_buttons.num(0)", "_buttons.num(1)");
		PORT_RENDER.register("_buttons.logic(0)", "_buttons.logic(1)", "_buttons.logic(2)", "_buttons.logic(3)");
		PORT_RENDER.register("_buttons.bin(0)", "_buttons.bin(1)", "_buttons.bin(2)", "_buttons.bin(3)");
		PORT_RENDER.register("_buttons.energy(0)", "_buttons.energy(1)");
		PORT_RENDER.register("_plug.main(0)", "_plug.main(1)", "_plug.main(2)", "_plug.main(3)", "_plug.main(4)", "_plug.main(5)", "_plug.main(6)", "_plug.main(7)");
		PORT_RENDER.register("_hook.pin(1)", "_hook.pin(2)", "_hook.pin(3)");
		PORT_RENDER.register("_battery");
		PORT_RENDER.dependencies.add(ItemSensor.MODEL);
		PORT_RENDER.dependencies.add(FluidSensor.MODEL);
		PORT_RENDER.dependencies.add(ForgeEnergySensor.MODEL);
		if (HAS_IC2_API)
			PORT_RENDER.dependencies.add(IC2EnergySensor.MODEL);
		
		registerRenderBS(RS_PORT, 0, 1);
		registerRender(WIRE_ANCHOR);
		registerRender(EDITOR);
		registerRender(ASSEMBLER);
		registerRender(wire);
		registerRender(wire_e);
		registerRender(wireless);
		registerRender(constant);
		registerRender(lamp);
		registerRender(tag);
		registerRender(block_wire);
		registerRender(clock);
	}

	private static void addGates(BlockGate... gates) {
		IStateMapper sm = new DefaultStateMapper();
		for (BlockGate gate : gates) {
			gate.setBlockLayer(BlockRenderLayer.CUTOUT);
			overrideBlockModel(gate, new MultipartModel(gate, sm.putStateModelLocations(gate), false, PORT_RENDER));
			registerRender(gate);
		}
	}

}
