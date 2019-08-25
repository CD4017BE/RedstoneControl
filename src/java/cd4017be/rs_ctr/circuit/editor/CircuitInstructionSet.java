package cd4017be.rs_ctr.circuit.editor;

import java.io.IOException;
import java.io.InputStream;
import com.google.gson.stream.JsonReader;

import cd4017be.lib.script.obj.Error;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Vector;
import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.editor.InstructionSet;
import cd4017be.rscpl.gui.Category;
import cd4017be.rscpl.util.IOUtils;
import net.minecraft.util.ResourceLocation;

/**
 * @author CD4017BE
 *
 */
public class CircuitInstructionSet extends InstructionSet implements IOperand {

	public static final CircuitInstructionSet INS_SET = new CircuitInstructionSet();
	public static Category[] TABS = new Category[10];
	public static int nextTabId = 0;

	/**
	 * register a tab for the gate palette in editor.<br>
	 * Its gate list is defined in {@code /assets/}<b>domain</b>{@code /logic/tabs/}<b>path</b>{@code .json}<br>
	 * Call this in pre init phase.
	 * 
	 * @param name file resource location {@code domain:path}
	 */
	public static void registerTab(String name) {
		TABS[nextTabId++] = new Category(name);
	}

	/** gate costs: [id] = 0xAABB (A_dvanced B_asic)*/
	public final char[] OP_COSTS = new char[256];

	public int getCost(GateType t) {
		return OP_COSTS[id(t)];
	}

	@Override
	public boolean asBool() throws Error {return true;}

	@Override
	public Object value() {return this;}

	@Override
	public void put(IOperand idx, IOperand val) {
		String key = idx.toString();
		if (key.indexOf(':') < 0) key = Main.ID + ':' + key;
		Integer id = IDS.get(key);
		if (id == null || !(val instanceof Vector)) return;
		double[] v = ((Vector)val).value;
		OP_COSTS[id] = (char)((int)v[0] & 0xff | (int)v[1] << 8);
	}

	public void loadTabs() {
		for (Category c : TABS) {
			if (c == null) continue;
			InputStream is = IOUtils.getClassResource(new ResourceLocation(c.name), "/logic/tabs/", ".json");
			if (is == null) {
				Main.LOG.error("missing tab definition for {}", c.name);
				continue;
			}
			try (JsonReader jr = IOUtils.readJson(is)) {
				jr.beginArray();
				while(jr.hasNext()) {
					GateType t = getType(jr.nextString());
					if (t != null) c.add(t);
				}
				jr.endArray();
			} catch(IOException | IllegalStateException e) {
				Main.LOG.error("failed to load tab definition of " + c.name, e);
			}
		}
	}

	public GateType getType(String name) {
		Integer id = IDS.get(name);
		if (id != null) return REGISTRY[id];
		InputStream is = IOUtils.getClassResource(new ResourceLocation(name), "/logic/gates/", ".json");
		if (is == null) {
			Main.LOG.error("missing gate definition for {}", name);
			return null;
		}
		try (JsonReader jr = IOUtils.readJson(is)) {
			return GeneratedType.read(name, jr, this);
		} catch(IOException e) {
			Main.LOG.error("failed to load gate definition of " + name, e);
			return null;
		}
	}

}
