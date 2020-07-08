package cd4017be.rs_ctr.circuit.data;

import java.util.function.Consumer;
import java.util.function.Supplier;
import cd4017be.lib.Gui.comp.*;
import cd4017be.lib.util.TooltipUtil;
import io.netty.buffer.ByteBuf;

/** 
 * @author CD4017BE */
public class VariantEnum implements GateConfiguration<Integer> {

	final int modes;

	public VariantEnum(int modes) {
		this.modes = modes;
	}

	@Override
	public int setupCfgGUI(GuiFrame gui, int y, Supplier<Integer> get, Consumer<Integer> set, String id) {
		String key = "gate." + id.replace(':', '.') + ".mode";
		new Button(
			gui, 76, 9, 0, y, 0, null,
			(a) -> set.accept((get.get() + (a == Button.B_LEFT || a == Button.B_SCROLL_DOWN ? 1 : modes - 1)) % modes)
		).texture(180, 109).tooltip(key);
		new FormatText(
			gui, 76, 9, 0, y, "\\%s",
			()-> new Object[] {TooltipUtil.translate(key + get.get())}
		).align(0.5F).color(0xff000000);
		return y + 9;
	}

	@Override
	public Integer init() {
		return 0;
	}

	@Override
	public void write(ByteBuf data, Object cfg) {
		data.writeByte((Integer)cfg);
	}

	@Override
	public Integer read(ByteBuf data) {
		return (data.readByte() & 0xff) % modes;
	}

}
