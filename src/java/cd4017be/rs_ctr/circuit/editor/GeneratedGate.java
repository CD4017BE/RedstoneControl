package cd4017be.rs_ctr.circuit.editor;

import java.util.Arrays;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.rs_ctr.circuit.data.GateConfiguration;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;
import cd4017be.rscpl.gui.ISpecialCfg;
import io.netty.buffer.ByteBuf;

/**
 * 
 * @author cd4017be
 *
 */
public class GeneratedGate extends Gate implements ISpecialCfg {

	protected final Node[] nodeCache;
	protected final Object[] cfg;

	public GeneratedGate(GeneratedType type, int index) {
		super(type, index, type.inputs.length, type.outputs.length);
		this.nodeCache = new Node[type.nodes.length + type.links];
		if (type.cfg.length > 0) {
			this.cfg = new Object[type.cfg.length];
			for (int i = 0; i < cfg.length; i++)
				cfg[i] = type.cfg[i].init();
		} else this.cfg = null;
	}

	@Override
	public void checkValid() throws InvalidSchematicException {
		Arrays.fill(nodeCache, null);
		super.checkValid();
	}

	public void setLink(Node n, int i) {
		if (i < ((GeneratedType)type).links)
			nodeCache[i] = n;
	}

	protected Node createLink(int i) {
		throw new UnsupportedOperationException();
	}

	public Node getEndNode() {
		GeneratedType type = (GeneratedType)this.type;
		return type.getNode(this, type.end);
	}

	public Object getParam(int i) {
		return cfg[i];
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public void setupCfgGUI(GuiFrame gui, Runnable updateCfg) {
		GateConfiguration[] gc = ((GeneratedType)type).cfg;
		int y = 9;
		for (int i = 0; i < gc.length; i++) {
			int i_ = i;
			y = gc[i].setupCfgGUI(gui, y, ()-> cfg[i_], (t)-> {cfg[i_] = t; updateCfg.run();});
		}
		gui.bgY = 45 - y;
	}

	@Override
	public boolean writeCfg(ByteBuf data) {
		if (cfg == null) return false;
		for (int i = 0; i < cfg.length; i++)
			((GeneratedType)type).cfg[i].write(data, cfg[i]);
		return true;
	}

	@Override
	public boolean readCfg(ByteBuf data) {
		if (cfg == null) return false;
		for (int i = 0; i < cfg.length; i++)
			cfg[i] = ((GeneratedType)type).cfg[i].read(data);
		return true;
	}

}
