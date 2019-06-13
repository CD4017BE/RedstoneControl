package cd4017be.rs_ctr.gui;

import org.lwjgl.input.Keyboard;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.FrameGrip;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Spinner;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rs_ctr.circuit.CompiledCircuit;
import cd4017be.rscpl.gui.StateEditor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GuiDebugger extends GuiFrame {

	private static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/palette.png");
	public final Circuit circuit;
	private int interval = 1, timer = Integer.MIN_VALUE;
	private int dirty = 1, cycles = 0;
	private final StateEditor state;
	private String lastErr = "";

	/**
	 * @param parent
	 * @param circuit
	 */
	public GuiDebugger(GuiFrame parent, CompiledCircuit circuit) {
		this(parent, circuit, circuit.inputs.length, circuit.outputs.length);
	}

	private GuiDebugger(GuiFrame parent, CompiledCircuit circuit, int in, int out) {
		super(parent, 168, 33, 6);
		this.circuit = circuit.load();
		background(TEX, 0, 114);
		title("gui.rs_ctr.debug.name", 0.5F);
		new FrameGrip(this, 8, 8, 0, 0);
		new Button(this, 8, 8, w - 8, 0, 0, null, (i)-> close()).tooltip("gui.cd4017be.close");
		new Spinner(this, 36, 18, 7, 15, false, "\\%.2fs", ()-> (double)interval / 20.0, (v)-> interval = (int)Math.round(v * 20.0), 0.05, 60.0, 1.0, 0.05).tooltip("gui.rs_ctr.interval");
		new Button(this, 18, 18, 43, 15, 2, ()-> timer < -interval ? 1 : 0, (s)-> timer = s == 0 ? -interval : Integer.MIN_VALUE).texture(168, 184).tooltip("gui.rs_ctr.debug.run#");
		new Button(this, 18, 18, 61, 15, 0, ()-> dirty & 1, this::tickChip).texture(168, 220).tooltip("gui.rs_ctr.debug.step#");
		new FormatText(this, 80, 8, 80, 16, "\\%s", ()-> new Object[] {lastErr});
		new FormatText(this, 62, 8, 80, 24, "\\cycle: %d", ()-> new Object[] {cycles});
		new Button(this, 62, 7, 80, 24, 0, null, ((b)-> cycles = 0)).tooltip("gui.rs_ctr.debug.reset");
		(state = StateEditor.of(this, this.circuit, circuit.ioLabels, 6, this::modify, true)).move(0, h);
		new Button(this, 18, 9, 143, 24, 2, ()-> state.hex ? 1 : 0, (s)-> state.hex = s != 0).texture(168, 166).tooltip("gui.rs_ctr.hex#");
	}

	private void modify(int var) {
		if (var < 0 && circuit.isInterrupt(-1 - var))
			dirty |= 1;
	}

	public void update() {
		if (++timer < 0) return;
		timer -= interval;
		tickChip(-1);
	}

	private void tickChip(int b) {
		if (b < 0 && (dirty & 1) == 0) return;
		try {
			dirty = circuit.tick();
			lastErr = "\u00a7ano error";
		} catch(Exception e) {
			if (!(e instanceof ArithmeticException))
				Main.LOG.error("circuit crashed!", e);
			dirty = 0;
			lastErr = "\u00a7c" + e.getLocalizedMessage();
		}
		cycles++;
		state.update();
	}

	public void close() {
		parent.remove(this);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		GlStateManager.disableDepth();
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
