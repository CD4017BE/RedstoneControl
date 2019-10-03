package cd4017be.rs_ctr.circuit.data;

import java.util.function.Consumer;
import java.util.function.Supplier;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import io.netty.buffer.ByteBuf;

/** @author CD4017BE */
public class ToggleFlag implements GateConfiguration<Boolean> {

	public static final ToggleFlag
		INTERRUPT = new ToggleFlag("gui.rs_ctr.interrupt#", 180, 73, true),
		SIGNED = new ToggleFlag("gui.rs_ctr.signed#", 180, 91, true);

	final String label;
	final int tx, ty;
	final boolean init;

	public ToggleFlag(String label, int tx, int ty, boolean init) {
		this.label = label;
		this.tx = tx;
		this.ty = ty;
		this.init = init;
	}

	@Override
	public int setupCfgGUI(
		GuiFrame gui, int y, Supplier<Boolean> get, Consumer<Boolean> set
	) {
		new Button(
			gui, 76, 9, 0, y, 2, () -> get.get() ? 1 : 0, (i) -> set.accept(i != 0)
		).texture(tx, ty).tooltip(label);
		return y + 9;
	}

	@Override
	public Boolean init() {
		return init;
	}

	@Override
	public void write(ByteBuf data, Object cfg) {
		data.writeBoolean((boolean)cfg);
	}

	@Override
	public Boolean read(ByteBuf data) {
		return data.readBoolean();
	}

}
