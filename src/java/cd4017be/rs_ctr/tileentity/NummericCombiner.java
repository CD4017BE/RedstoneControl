package cd4017be.rs_ctr.tileentity;

import java.util.Collections;
import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.gui.BlockButton;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author CD4017BE
 *
 */
public class NummericCombiner extends SignalCombiner {

	byte inModes;
	BlockButton[] buttons = new BlockButton[4];
	{
		for (int i = 0; i < 4; i++) {
			int pin = i;
			buttons[i] = new BlockButton(
				(a)-> {
					inModes ^= 1 << pin;
					refreshInput(pin);
					markDirty(REDRAW);
				},
				()-> "_buttons.num(" + (inModes >> pin & 1) + ")",
				()-> TooltipUtil.translate("port.rs_ctr.num" + (inModes >> pin & 1))
			).setSize(0.0625F, 0.0625F);
		}
	}

	@Override
	public void process() {
		int val = inputs[0] + inputs[1] + inputs[2] + inputs[3];
		super.process();
		setOutput(val);
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		if ((inModes >> pin & 1) == 0)
			return super.getPortCallback(pin);
		return (val)-> setInput(pin, -val);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		nbt.setByte("mode", inModes);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		inModes = nbt.getByte("mode");
		super.loadState(nbt, mode);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		Collections.addAll(list, buttons);
	}

	@Override
	protected void orient(Orientation o) {
		for (int i = 0; i < 4; i++)
			buttons[i].setLocation(0.375F, 0.125F + i * 0.25F, 0.25F, o);
		super.orient(o);
	}

}
