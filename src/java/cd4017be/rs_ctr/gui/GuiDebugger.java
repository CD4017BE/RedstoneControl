package cd4017be.rs_ctr.gui;

import org.lwjgl.input.Keyboard;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.FrameGrip;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Spinner;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rs_ctr.circuit.CompiledCircuit;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;


/**
 * @author CD4017BE
 *
 */
public class GuiDebugger extends GuiFrame {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/palette.png");
	public Circuit circuit;
	private int interval = 1, timer = Integer.MIN_VALUE;
	private int dirty = 1;

	/**
	 * @param parent
	 * @param circuit
	 */
	public GuiDebugger(GuiFrame parent, CompiledCircuit circuit) {
		this(parent, circuit, circuit.inputs.length, circuit.outputs.length);
	}

	private GuiDebugger(GuiFrame parent, CompiledCircuit circuit, int in, int out) {
		super(parent, 86, 40 + (in + out) * 18, 5 + in * 2 + out);
		this.circuit = circuit.load();
		texture(TEX, 256, 256);
		title("gui.rs_ctr.debug.name", 0.5F);
		new FrameGrip(this, 8, 8, 0, 0);
		new Button(this, 8, 8, w - 8, 0, 0, null, (i)-> close()).tooltip("gui.cd4017be.close");
		new Spinner(this, 36, 18, 7, 15, false, "\\%.2fs", ()-> (double)interval / 20.0, (v)-> interval = (int)Math.round(v * 20.0), 0.05, 60.0, 1.0, 0.05).tooltip("gui.rs_ctr.interval");
		new Button(this, 18, 18, 43, 15, 2, ()-> timer < -interval ? 1 : 0, (s)-> timer = s == 0 ? -interval : Integer.MIN_VALUE).texture(86, 184).tooltip("gui.rs_ctr.debug.run#");
		new Button(this, 18, 18, 61, 15, 0, ()-> dirty & 1, this::tickChip).texture(86, 220).tooltip("gui.rs_ctr.debug.step#");
		for (int i = 0; i < in; i++) {
			final String label = circuit.ioLabels[i];
			final int idx = i;
			new FormatText(this, 70, 9, 8, 34 + i * 18, "\\" + label, null).color(0xff00007f);
			new TextField(this, 70, 7, 8, 43 + i * 18, 16, ()-> "" + this.circuit.inputs[idx], (t)-> {try {
					int v = Integer.parseInt(t);
					if (this.circuit.isInterrupt(idx) && v != this.circuit.inputs[idx]) dirty |= 1;
					this.circuit.inputs[idx] = v;
				} catch (NumberFormatException e) {}});
		}
		for (int i = 0; i < out; i++) {
			final String label = circuit.ioLabels[in + i];
			final int idx = i;
			new FormatText(this, 70, 9, 8, 34 + (i + in) * 18, "\\" + label.replace("%", "%%") + "\n%d", ()-> new Object[] {circuit.outputs[idx]}).color(0xff007f00);
		}
		//TODO state
	}

	public void update() {
		if (++timer < 0) return;
		timer -= interval;
		tickChip(0);
	}

	private void tickChip(int b) {
		if ((dirty & 1) == 0) return;
		try {
			dirty = circuit.tick();
		} catch(Exception e) {
			Main.LOG.error("circuit crashed!", e);
			dirty = 0;
		}
	}

	public void close() {
		parent.remove(this);
		//parent.remove(variables);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		if (parent != null) parent.drawNow();
		GlStateManager.disableDepth();
		int y = this.y;
		bindTexture(mainTex);
		GuiUtils.drawTexturedModalRect(x, y, 0, 180, 86, 33, zLevel);
		y += 33;
		for (int n = circuit.inputs.length; n > 0; n--, y += 18)
			GuiUtils.drawTexturedModalRect(x, y, 0, 213, 86, 18, zLevel);
		for (int n = circuit.outputs.length; n > 0; n--, y += 18)
			GuiUtils.drawTexturedModalRect(x, y, 0, 231, 86, 18, zLevel);
		GuiUtils.drawTexturedModalRect(x, y, 0, 249, 86, 7, zLevel);
		super.drawBackground(mx, my, t);
		GlStateManager.enableDepth();
	}

	@Override
	public boolean keyIn(char c, int k, byte d) {
		if (k == Keyboard.KEY_ESCAPE) {
			close();
			return true;
		}
		return super.keyIn(c, k, d);
	}

}
