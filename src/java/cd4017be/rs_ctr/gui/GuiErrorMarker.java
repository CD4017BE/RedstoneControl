package cd4017be.rs_ctr.gui;

import java.util.Arrays;

import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.tileentity.Editor;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;
import static cd4017be.rscpl.editor.InvalidSchematicException.*;


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
		if (e == NO_ERROR) {
			lastErr = null;
			setEnabled(false);
		} else if (lastErr == null || lastErr.errcode != (e & 0xff)) {
			lastErr = new InvalidSchematicException(e, gui.tile.schematic::get);
			if (lastErr.gate != null) {
				gui.board.selPart = lastErr.gate.getBounds();
				gui.changeSelPart();
			}
			setEnabled(true);
		}
	}

	@Override
	public void drawOverlay(int mx, int my) {
		InvalidSchematicException lastErr = this.lastErr;
		if (lastErr == null) return;
		Gate<?> node = lastErr.gate;
		int px, py;
		switch(lastErr.errcode) {
		case Editor.MISSING_IO:
		case Editor.NO_CIRCUITBOARD:
			px = 182;
			py = 240;
			break;
		case Editor.MISSING_RESOURCE:
			px = 220;
			py = 234 + lastErr.pin * 6;
			break;
		case TYPE_MISSMATCH:
		case INVALID_LABEL:
		case Editor.MISSING_IO_LABEL:
			px = 211;
			py = 177;
			break;
		case INVALID_CFG:
			px = 211;
			py = 188;
			break;
		case READ_CONFLICT:
		case WRITE_CONFLICT:
			if (node == null) return;
			px = (node.rasterX << 2) + 14;
			py = (node.rasterY << 2) + 19;
			break;
		case CAUSAL_LOOP:
		case MISSING_INPUT:
			if (node == null) return;
			if (lastErr.pin < node.visibleInputs()) {
				px = (node.rasterX << 2) + 10;
				py = ((node.rasterY + node.getInputHeight(lastErr.pin)) << 2) + 18;
			} else {
				px = (node.rasterX << 2) + 14;
				py = (node.rasterY << 2) + 19;
			}
			break;
		default: return;
		}
		gui.mc.renderEngine.bindTexture(parent.mainTex);
		gui.drawTexturedModalRect(x + px - 4, y + py - 8, 248, 18, 8, 8);
		parent.drawTooltip(Arrays.asList(TooltipUtil.getConfigFormat("gui.rs_ctr.error" + lastErr.errcode).split("\n")), x + px, y + py);
	}

}
