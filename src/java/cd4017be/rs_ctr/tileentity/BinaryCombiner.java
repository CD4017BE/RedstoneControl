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
public class BinaryCombiner extends SignalCombiner {

	int mask = 1, shift = 1;
	BlockButton button = new BlockButton(
		(a)-> {
			if ((a & BlockButton.A_SNEAKING) != 0) {
				if ((shift >>>= 1) == 0) shift = 8;
			} else if ((shift <<= 1) > 8) shift = 1;
			mask = 0xff >>> (8-shift);
			for (int i = 0; i < 4; i++)
				refreshInput(i);
			markDirty(REDRAW);
		},
		()-> "_buttons.bin(" + Integer.numberOfTrailingZeros(shift) + ")",
		()-> TooltipUtil.translate("port.rs_ctr.bin" + shift)
	).setSize(0.0625F, 0.125F);

	{
		for (int i = 0; i < 4; i++)
			ports[i].setName("port.rs_ctr.i" + i);
	}

	@Override
	public void process() {
		int v = inputs[0] | inputs[1] | inputs[2] | inputs[3];
		super.process();
		setOutput(v);
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
		return (val)-> setInput(pin, val <= 0 ? 0 : (val > mask ? mask : val) << (shift * pin));
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
		button.setLocation(0.625F, 0.5F, 0.25F, o);
		super.orient();
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(button);
	}

}
