package cd4017be.rs_ctr;

import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.api.rs_ctr.sensor.SensorRegistry;
import cd4017be.lib.render.model.BlockMimicModel;
import cd4017be.lib.render.model.MultipartModel;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.sensor.BlockHardnessSensor;
import cd4017be.rs_ctr.sensor.DraconicFusionSensor;
import cd4017be.rs_ctr.sensor.FluidSensor;
import cd4017be.rs_ctr.sensor.ForgeEnergySensor;
import cd4017be.rs_ctr.sensor.GrowthSensor;
import cd4017be.rs_ctr.sensor.IC2EnergySensor;
import cd4017be.rs_ctr.sensor.ItemSensor;
import cd4017be.rs_ctr.sensor.LightSensor;
import static cd4017be.rs_ctr.render.PortRenderer.PORT_RENDER;
import static cd4017be.rs_ctr.render.FrameRenderer.FRAME_RENDER;

import cd4017be.rs_ctr.block.BlockGate;
import cd4017be.rs_ctr.tileentity.BlockSelector;
import cd4017be.rs_ctr.tileentity.Gate;
import cd4017be.rscpl.gui.Category;
import cd4017be.rscpl.gui.GateTextureHandler;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import static net.minecraftforge.fml.client.registry.ClientRegistry.*;

import java.util.Collections;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import static cd4017be.rs_ctr.Objects.*;
import static cd4017be.lib.BlockItemRegistry.*;
import static cd4017be.lib.render.SpecialModelLoader.*;

/**
 *
 * @author CD4017BE
 */
public class ClientProxy extends CommonProxy {

	public static final ResourceLocation 
	T_BLANK = new ResourceLocation("white"),
	T_DIAL = new ResourceLocation(Main.ID, "blocks/analog_dial"),
	T_7SEG = new ResourceLocation(Main.ID, "blocks/7seg"),
	T_SOCKETS = new ResourceLocation(Main.ID, "blocks/sockets"),
	T_FRAME = new ResourceLocation(Main.ID, "blocks/area"),
	T_BEAM = new ResourceLocation(Main.ID, "blocks/beam"),
	T_OSC = new ResourceLocation(Main.ID, "blocks/oscilloscope");
	public static TextureAtlasSprite t_blank, t_dial, t_7seg, t_sockets, t_frame, t_beam, t_osc;

	@Override
	public void preInit() {
		super.preInit();
		SensorRegistry.RENDERER = PORT_RENDER;
	}

	@Override
	public void init(ConfigConstants cc) {
		super.init(cc);
		bindTileEntitySpecialRenderer(Gate.class, PORT_RENDER);
		bindTileEntitySpecialRenderer(BlockSelector.class, FRAME_RENDER);
		
		for (Category c : CircuitInstructionSet.TABS)
			if (c != null)
				GateTextureHandler.ins_sets.add(c);
		GateTextureHandler.register();
	}

	@SubscribeEvent
	public void registerTextures(TextureStitchEvent.Pre event) {
		TextureMap map = event.getMap();
		if (!"textures".equals(map.getBasePath())) return;
		map.registerSprite(T_DIAL);
		map.registerSprite(T_7SEG);
		map.registerSprite(T_SOCKETS);
		map.registerSprite(T_FRAME);
		map.registerSprite(T_BEAM);
		map.registerSprite(T_OSC);
	}

	@SubscribeEvent
	public void getTextures(TextureStitchEvent.Post event) {
		TextureMap map = event.getMap();
		if (!"textures".equals(map.getBasePath())) return;
		t_blank = map.getAtlasSprite(T_BLANK.toString());
		t_dial = map.getAtlasSprite(T_DIAL.toString());
		t_7seg = map.getAtlasSprite(T_7SEG.toString());
		t_sockets = map.getAtlasSprite(T_SOCKETS.toString());
		t_frame = map.getAtlasSprite(T_FRAME.toString());
		t_beam = map.getAtlasSprite(T_BEAM.toString());
		t_osc = map.getAtlasSprite(T_OSC.toString());
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent ev) {
		setMod(Main.ID);
		
		registerBlockModel(RS_PORT, new MultipartModel(RS_PORT).setPipeVariants(1).setProvider(7, PORT_RENDER));
		overrideBlockModel(WIRE_ANCHOR, new MultipartModel(WIRE_ANCHOR, Collections.singletonMap(WIRE_ANCHOR.getDefaultState(), new ModelResourceLocation(WIRE_ANCHOR.getRegistryName(), "empty")), true, PORT_RENDER, BlockMimicModel.provider));
		addGates(SPLITTER, ANALOG_COMB, LOGIC_COMB, NUM_COMB, BIN_COMB, BIN_SPLIT, XOR_GATE, COUNTER, DELAY, SPLITTER_B, MULTIPLEX_B, DELAY_B, PROCESSOR, PROCESSOR2, COMPARATOR, POWER_HUB, ITEM_TRANSLOCATOR, FLUID_TRANSLOCATOR, ENERGY_VALVE, PANEL, SOLAR_CELL, BLOCK_SELECTOR, BLOCK_BREAKER, ITEM_PLACER, OC_ADAPTER, RAM);
		
		WIRE_ANCHOR.setBlockLayer(null);
		
		PORT_RENDER.register("_buttons.num(0)", "_buttons.num(1)");
		PORT_RENDER.register("_buttons.logic(0)", "_buttons.logic(1)", "_buttons.logic(2)", "_buttons.logic(3)");
		PORT_RENDER.register("_buttons.bin(0)", "_buttons.bin(1)", "_buttons.bin(2)", "_buttons.bin(3)");
		PORT_RENDER.register("_buttons.energy(0)", "_buttons.energy(1)");
		PORT_RENDER.register("_plug.main(0)", "_plug.main(1)", "_plug.main(2)", "_plug.main(3)", "_plug.main(4)", "_plug.main(5)", "_plug.main(6)", "_plug.main(7)");
		PORT_RENDER.register("_hook.pin(1)", "_hook.pin(2)", "_hook.pin(3)");
		PORT_RENDER.register("_battery");
		PORT_RENDER.register("_lever.on", "_lever.off", "_lever.btn");
		PORT_RENDER.dependencies.add(ItemSensor.MODEL);
		PORT_RENDER.dependencies.add(FluidSensor.MODEL);
		PORT_RENDER.dependencies.add(ForgeEnergySensor.MODEL);
		PORT_RENDER.dependencies.add(BlockHardnessSensor.MODEL);
		PORT_RENDER.dependencies.add(LightSensor.MODEL);
		PORT_RENDER.dependencies.add(GrowthSensor.MODEL);
		if (HAS_IC2_API)
			PORT_RENDER.dependencies.add(IC2EnergySensor.MODEL);
		if (Loader.isModLoaded("draconicevolution"))
			PORT_RENDER.dependencies.add(DraconicFusionSensor.MODEL);
		
		registerRender(RS_PORT, 0, 3);
		registerRender(WIRE_ANCHOR);
		registerRender(EDITOR);
		registerRender(ASSEMBLER);
		registerRender(FRAME);
		registerRender(wire);
		registerRender(wire_e);
		registerRender(wireless);
		registerRender(constant);
		registerRender(lamp);
		registerRender(tag);
		registerRender(block_wire);
		registerRender(clock);
		registerRender(wrench);
		registerRender(seg7);
		registerRender(pointer_dsp);
		registerRender(slider);
		registerRender(text, 1, 4);
		registerRender(lever, 0, 2);
		registerRender(trigger);
		registerRender(scale);
		registerRender(offset);
		registerRender(oscilloscope);
		registerRender(ram, null);
	}

	private static void addGates(BlockGate... gates) {
		IStateMapper sm = new DefaultStateMapper();
		for (BlockGate gate : gates) {
			gate.setBlockLayer(null);
			overrideBlockModel(gate, new MultipartModel(gate, sm.putStateModelLocations(gate), true, PORT_RENDER, BlockMimicModel.provider));
			registerRender(gate);
		}
	}

}
