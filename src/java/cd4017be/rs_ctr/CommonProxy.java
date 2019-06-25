package cd4017be.rs_ctr;

import cd4017be.api.recipes.RecipeScriptContext;

import java.util.HashMap;

import cd4017be.api.recipes.RecipeAPI;
import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.util.ItemKey;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.signal.BlockProbe;
import cd4017be.rs_ctr.signal.Clock;
import cd4017be.rs_ctr.signal.Constant;
import cd4017be.rs_ctr.signal.StatusLamp;
import cd4017be.rs_ctr.signal.WireType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy implements IRecipeHandler {

	public static final String CIRCUIT_MAT = "circuitMat";
	public static final int[] NULL = new int[6];
	public static final HashMap<ItemKey, int[]> MATERIALS = new HashMap<>();

	/**
	 * @param stack processor ingredient
	 * @return {basicCmplx, advCmplx, memory, size, gain, cap}
	 */
	public static int[] getStats(ItemStack stack) {
		int[] e = MATERIALS.get(new ItemKey(stack));
		return e == null ? NULL : e;
	}

	@Override
	public void addRecipe(Parameters param) {
		ItemKey key = new ItemKey(param.get(1, ItemStack.class));
		double[] arr = param.getVectorOrAll(2);
		int[] stats = new int[NULL.length];
		int n = Math.min(arr.length, stats.length);
		for (int i = 0; i < n; i++)
			stats[i] = (int)arr[i];
		MATERIALS.put(key, stats);
	}

	public void preInit() {
		MinecraftForge.EVENT_BUS.register(this);
		RecipeScriptContext.instance.modules.get("redstoneControl").assign("gate_cost", CircuitInstructionSet.INS_SET);
		RecipeAPI.Handlers.put(CIRCUIT_MAT, this);
	}

	public void init() {
		WireType.registerAll();
		IConnector.REGISTRY.put(Constant.ID, Constant::new);
		IConnector.REGISTRY.put(StatusLamp.ID, StatusLamp::new);
		IConnector.REGISTRY.put(BlockProbe.ID, BlockProbe::new);
		IConnector.REGISTRY.put(Clock.ID, Clock::new);
	}

}
