package cd4017be.rs_ctr.gui;

import java.util.Arrays;

import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;
import cd4017be.rscpl.editor.InvalidSchematicException.ErrorType;


/**
 * @author CD4017BE
 *
 */
public class GuiErrorMarker extends GuiCompBase<GuiCompGroup> {

	private final CircuitEditor gui;
	public InvalidSchematicException lastErr;
	

	/**
	 * @param parent
	 */
	public GuiErrorMarker(CircuitEditor gui) {
		super(gui.compGroup, gui.compGroup.w, gui.compGroup.h, 0, 0);
		this.gui = gui;
	}

	public void update(int e) {
		ErrorType t = ErrorType.get(e);
		if (t == null) {
			lastErr = null;
			setEnabled(false);
		} else if (lastErr == null || lastErr.type != t) {
			int n = e >> 8 & 0xffff;
			Gate<?> gate = gui.tile.schematic.get(n);
			if (gate != null) {
				gui.board.selPart = gate.getBounds();
				gui.changeSelPart();
			}
			lastErr = new InvalidSchematicException(t, gate, e >> 24 & 0xff);
			setEnabled(true);
		}
	}

	@Override
	public void drawOverlay(int mx, int my) {
		InvalidSchematicException lastErr = this.lastErr;
		if (lastErr == null) return;
		Gate<?> node = lastErr.node;
		int px, py;
		switch(lastErr.type) {
		case noCircuitBoard:
			px = 182;
			py = 240;
			break;
		case missingMaterial:
			px = 220;
			py = 234 + lastErr.pin * 6;
			break;
		case typeMissmatch:
		case invalidLabel:
			px = 211;
			py = 177;
			break;
		case invalidCfg:
			px = 211;
			py = 188;
			break;
		case readConflict:
		case writeConflict:
			if (node == null) return;
			px = (node.rasterX << 2) + 14;
			py = (node.rasterY << 2) + 19;
			break;
		case causalLoop:
		case missingInput:
			if (node == null) return;
			if (lastErr.pin < node.inputCount()) {
				px = (node.rasterX << 2) + 10;
				py = ((node.rasterY + node.getInputHeight(lastErr.pin)) << 2) + 18;
				break;
			}
		default: return;
		}
		gui.mc.renderEngine.bindTexture(parent.mainTex);
		gui.drawTexturedModalRect(x + px - 4, y + py - 8, 248, 236, 8, 8);
		parent.drawTooltip(Arrays.asList(TooltipUtil.getConfigFormat("gui.circuits." + lastErr.type.name()).split("\n")), x + px, y + py);
	}

}
