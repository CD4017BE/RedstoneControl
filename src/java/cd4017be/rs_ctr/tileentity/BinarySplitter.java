package cd4017be.rs_ctr.tileentity;

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
public class BinarySplitter extends SignalSplitter {

	int mask = 1, shift = 1;
	BlockButton button = new BlockButton(
		(a)-> {
			if ((a & BlockButton.A_SNEAKING) != 0) {
				if ((shift >>>= 1) == 0) shift = 8;
			} else if ((shift <<= 1) > 8) shift = 1;
			mask = 0xff >>> (8-shift);
			for (int i = 0; i < 4; i++)
				setPortCallback(i, callbacks[i]);
			markDirty(REDRAW);
		},
		()-> "_buttons.bin(" + Integer.numberOfTrailingZeros(shift) + ")",
		()-> TooltipUtil.translate("port.rs_ctr.bins" + shift)
	).setSize(0.0625F, 0.125F);

	{
		for (int i = 0; i < 4; i++)
			ports[i].setName("port.rs_ctr.o" + i);
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
		return (val)-> {
			if (val == state) return;
			state = val;
			for (int i = 0, s = shift, m = mask; i < 4; i++, val >>>= s) {
				IntConsumer c = callbacks[i];
				if (c != null) c.accept(val & m);
			}
		};
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		IntConsumer scb = callback instanceof IntConsumer ? (IntConsumer)callback : null;
		callbacks[pin] = scb;
		if (scb != null) scb.accept(state >>> (shift * pin) & mask);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		nbt.setByte("bits", (byte)shift);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		shift = Integer.highestOneBit(nbt.getByte("bits") & 15 | 1);
		mask = 0xff >>> (8-shift);
		super.loadState(nbt, mode);
	}

	@Override
	protected void orient() {
		button.setLocation(0.375F, 0.5F, 0.25F, o);
		super.orient();
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(button);
	}

}
