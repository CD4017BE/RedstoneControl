package cd4017be.rs_ctr.tileentity;

import java.util.Arrays;
import java.util.function.IntConsumer;

import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.gui.BlockButton;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author CD4017BE
 *
 */
public class NummericCombiner extends SignalCombiner {

	IInteractiveComponent[] gui = ports;
	byte inModes;

	@Override
	public void process() {
		dirty = false;
		int val = inputs[0]
				+ inputs[1]
				+ inputs[2]
				+ inputs[3];
		output.accept(val);
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
		if ((inModes >> pin & 1) == 0)
			return super.getPortCallback(pin);
		return (val)-> {
			val = -val;
			if (val != inputs[pin]) {
				inputs[pin] = val;
				scheduleUpdate();
			}
		};
	}

	@Override
	protected void writePorts(NBTTagCompound nbt) {
		super.writePorts(nbt);
		nbt.setByte("mode", inModes);
	}

	@Override
	protected void readPorts(NBTTagCompound nbt) {
		super.readPorts(nbt);
		inModes = nbt.getByte("mode");
	}

	@Override
	public IInteractiveComponent[] getComponents() {
		return gui;
	}

	@Override
	protected void orient() {
		super.orient();
		gui = Arrays.copyOf(ports, 9, IInteractiveComponent[].class);
		for (int i = 0; i < 4; i++) {
			int pin = i;
			gui[i + 5] = new BlockButton(
				(a)-> {
					inModes ^= 1 << pin;
					inputs[pin] *= -1;
					markDirty();
					scheduleUpdate();
					markUpdate();
				},
				()-> "plug.num(" + (inModes >> pin & 1) + ")",
				()-> TooltipUtil.translate("port.rs_ctr.num" + (inModes >> pin & 1))
			).setSize(0.0625F, 0.0625F).setLocation(0.375F, 0.125F + i * 0.25F, 0.25F, o);
		}
	}

}
