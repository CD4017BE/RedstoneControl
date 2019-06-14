package cd4017be.rs_ctr.tileentity;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.gui.BlockButton;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author CD4017BE
 *
 */
public class LogicCombiner extends SignalCombiner {

	byte[] inModes = new byte[4];
	int outInv;
	BlockButton[] buttons = new BlockButton[5];
	{
		for (int i = 0; i < 4; i++) {
			int pin = i;
			buttons[i] = new BlockButton(
				(a)-> {
					inModes[pin] = (byte)((inModes[pin] + ((a & BlockButton.A_SNEAKING) != 0 ? -1 : 1)) & 3);
					refreshInput(pin);
					markDirty(REDRAW);
				},
				()-> "_buttons.logic(" + inModes[pin] + ")",
				()-> TooltipUtil.translate("port.rs_ctr.logic" + inModes[pin])
			).setSize(0.0625F, 0.0625F);
		}
		buttons[4] = new BlockButton(
			(a)-> {
				outInv ^= 0xffff;
				scheduleUpdate();
				markDirty(REDRAW);
			},
			()-> "_buttons.logic(" + (outInv != 0 ? 3 : 2) + ")",
			()-> TooltipUtil.translate("port.rs_ctr.logic" + (outInv != 0 ? 5 : 4))
		).setSize(0.0625F, 0.0625F);
	}

	@Override
	public void process() {
		int val = inputs[0] | inputs[1] | inputs[2] | inputs[3];
		super.process();
		output.accept(val ^ outInv);
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
		switch(inModes[pin]) {
		case 0: return (val)-> setInput(pin, val > 0 ? 0xffff : 0);
		case 1: return (val)-> setInput(pin, val <= 0 ? 0xffff : 0);
		case 2: return (val)-> setInput(pin, val & 0xffff);
		default: return (val)-> setInput(pin, ~val & 0xffff);
		}
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		nbt.setShort("mode", (short)(inModes[0] | inModes[1] << 2 | inModes[2] << 4 | inModes[3] << 6 | outInv & 0x100));
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		int m = nbt.getShort("mode");
		for (int i = 0; i < inModes.length; i++)
			inModes[i] = (byte)(m >> i * 2 & 3);
		outInv = (m & 0x100) != 0 ? 0xffff : 0;
		super.loadState(nbt, mode);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		Collections.addAll(list, buttons);
	}

	@Override
	protected void orient() {
		for (int i = 0; i < 4; i++)
			buttons[i].setLocation(0.375F, 0.125F + i * 0.25F, 0.25F, o);
		buttons[4].setLocation(0.625F, 0.5F, 0.25F, o);
		super.orient();
	}

}
