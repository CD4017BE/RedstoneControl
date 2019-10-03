package cd4017be.rs_ctr.circuit.data;

import java.util.function.Consumer;
import java.util.function.Supplier;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import io.netty.buffer.ByteBuf;

/** @author CD4017BE */
public class IntegerValue implements GateConfiguration<Integer> {

	public static final IntegerValue VALUE
	= new IntegerValue("gui.rs_ctr.value");

	final String label;

	public IntegerValue(String label) {
		this.label = label;
	}

	@Override
	public int setupCfgGUI(
		GuiFrame gui, int y, Supplier<Integer> get, Consumer<Integer> set
	) {
		new TextField(
			gui, 74, 7, 1, y + 1, 20, () -> get.get().toString(), (s) -> {
				try {
					set.accept(Integer.parseInt(s));
				} catch(NumberFormatException e) {}
			}
		).tooltip(label);
		return y + 9;
	}

	@Override
	public Integer init() {
		return 0;
	}

	@Override
	public void write(ByteBuf data, Object cfg) {
		data.writeInt((int)cfg);
	}

	@Override
	public Integer read(ByteBuf data) {
		return data.readInt();
	}

}
