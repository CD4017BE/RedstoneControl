package cd4017be.rs_ctr.gui;

import java.io.IOException;
import static org.lwjgl.input.Keyboard.*;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.*;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.item.ItemRemoteControl.StateInteractionHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;


/** 
 * @author CD4017BE */
public class GuiRemoteKeybind extends ModularGui {

	static final ResourceLocation TEX = new ResourceLocation(Main.ID, "textures/gui/remote.png");
	final StateInteractionHandler state;
	int sel = -1;

	public GuiRemoteKeybind(StateInteractionHandler state, EntityPlayer player) {
		super(state.createContainer(player));
		this.state = state;
		GuiFrame frame = new GuiFrame(this, 168, 166, 64).background(TEX, 0, 0).title("gui.rs_ctr.remote.name", 0.5F);
		for (int i = 0; i < 32; i++) {
			int idx = i, x = 7 + (i >> 4) * 79, y = 15 + (i & 15) * 9;
			new Button(frame, 51, 9, x, y, 0, ()-> sel == idx ? 1 : 0, (b)-> {
				if (sel >= 0) sendPkt((byte)sel, (byte)KEY_NONE);
				sel = sel == idx ? -1 : idx;
			}).texture(205, 0).tooltip("gui.rs_ctr.remote.key#");
			new FormatText(frame, 51, 8, x, y + 1, "\\%s", ()-> new Object[] {getKeyName(state.keys[idx] & 0xff)}).align(0.5F);
		}
		this.compGroup = frame;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (sel >= 0) {
			sendPkt((byte)sel, (byte)keyCode);
			sel = -1;
		} else super.keyTyped(typedChar, keyCode);
	}

}
