package cd4017be.rs_ctr.circuit.data;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import cd4017be.lib.Gui.comp.GuiFrame;
import io.netty.buffer.ByteBuf;

/** @author CD4017BE */
public interface GateConfiguration<T> {

	public static final HashMap<String, GateConfiguration<?>> REGISTRY
	= new HashMap<>();

	int setupCfgGUI(GuiFrame gui, int y, Supplier<T> get, Consumer<T> set);

	T init();

	void write(ByteBuf data, Object cfg);

	T read(ByteBuf data);

}
