package cd4017be.rs_ctr;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.render.model.MultipartModel;
import cd4017be.rs_ctr.api.interact.InteractiveDeviceRenderer;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.gui.CircuitEditor;
import cd4017be.rs_ctr.render.WireRenderer;
import cd4017be.rs_ctr.tileentity.Gate;
import cd4017be.rscpl.gui.Category;
import cd4017be.rscpl.gui.GateTextureHandler;
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
		BlockGuiHandler.registerGui(EDITOR, CircuitEditor.class);
		
		bindTileEntitySpecialRenderer(Gate.class, new InteractiveDeviceRenderer());
		
		GateTextureHandler.register();
		for (Category c : CircuitInstructionSet.TABS)
			GateTextureHandler.ins_sets.add(c);
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent ev) {
		setMod(Main.ID);
		registerBlockModel(RS_PORT, new MultipartModel(RS_PORT).setPipeVariants(4));
		
		WireRenderer.register();
		
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
