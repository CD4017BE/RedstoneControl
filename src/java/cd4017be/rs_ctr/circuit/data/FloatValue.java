package cd4017be.rs_ctr.circuit.data;

import java.util.function.Consumer;
import java.util.function.Supplier;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import io.netty.buffer.ByteBuf;

/** @author CD4017BE */
public class FloatValue implements GateConfiguration<Float> {

	public static final FloatValue VALUE = new FloatValue("gui.rs_ctr.value");

	final String label;

	public FloatValue(String label) {
		this.label = label;
	}

	@Override
	public int setupCfgGUI(
		GuiFrame gui, int y, Supplier<Float> get, Consumer<Float> set, String id
	) {
		new TextField(
			gui, 74, 7, 1, y + 1, 20, () -> get.get().toString(), (s) -> {
				try {
					set.accept(Float.parseFloat(s));
				} catch(NumberFormatException e) {}
			}
		).tooltip(label);
		return y + 9;
	}

	@Override
	public Float init() {
		return 0.0F;
	}

	@Override
	public void write(ByteBuf data, Object cfg) {
		data.writeFloat((float)cfg);
	}

	@Override
	public Float read(ByteBuf data) {
		return data.readFloat();
	}

}
