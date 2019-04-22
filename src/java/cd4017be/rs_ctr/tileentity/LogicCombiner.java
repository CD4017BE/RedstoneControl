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
public class LogicCombiner extends SignalCombiner {

	IInteractiveComponent[] gui = ports;
	byte[] inModes = new byte[4];
	int outInv;

	@Override
	public void process() {
		dirty = false;
		int val = inputs[0]
				| inputs[1]
				| inputs[2]
				| inputs[3];
		output.accept(val ^ outInv);
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
		switch(inModes[pin]) {
		case 0:
			return (val)-> {
				val = val > 0 ? 0xffff : 0;
				if (val != inputs[pin]) {
					inputs[pin] = val;
					scheduleUpdate();
				}
			};
		case 1:
			return (val)-> {
				val = val <= 0 ? 0xffff : 0;
				if (val != inputs[pin]) {
					inputs[pin] = val;
					scheduleUpdate();
				}
			};
		case 2:
			return (val)-> {
				val &= 0xffff;
				if (val != inputs[pin]) {
					inputs[pin] = val;
					scheduleUpdate();
				}
			};
		default:
			return (val)-> {
				val = (val ^ -1) & 0xffff;
				if (val != inputs[pin]) {
					inputs[pin] = val;
					scheduleUpdate();
				}
			};
		}
	}

	@Override
	protected void writePorts(NBTTagCompound nbt) {
		super.writePorts(nbt);
		nbt.setShort("mode", (short)(inModes[0] | inModes[1] << 2 | inModes[2] << 4 | inModes[3] << 6 | outInv & 0x100));
	}

	@Override
	protected void readPorts(NBTTagCompound nbt) {
		super.readPorts(nbt);
		int mode = nbt.getShort("mode");
		for (int i = 0; i < inModes.length; i++)
			inModes[i] = (byte)(mode >> i * 2 & 3);
		outInv = (mode & 0x100) != 0 ? 0xffff : 0;
	}

	@Override
	public IInteractiveComponent[] getComponents() {
		return gui;
	}

	@Override
	protected void orient() {
		super.orient();
		gui = Arrays.copyOf(ports, 10, IInteractiveComponent[].class);
		for (int i = 0; i < 4; i++) {
			int pin = i;
			gui[i + 5] = new BlockButton(
				(a)-> {
					inModes[pin] = (byte)((inModes[pin] + ((a & BlockButton.A_SNEAKING) != 0 ? -1 : 1)) & 3);
					refreshInput(pin);
					markUpdate();
				},
				()-> "plug.logic(" + inModes[pin] + ")",
				()-> TooltipUtil.translate("port.rs_ctr.logic" + inModes[pin])
			).setSize(0.0625F, 0.0625F).setLocation(0.375F, 0.125F + i * 0.25F, 0.25F, o);
		}
		gui[9] = new BlockButton(
			(a)-> {
				outInv ^= 0xffff;
				scheduleUpdate();
				markDirty();
				markUpdate();
			},
			()-> "plug.logic(" + (outInv != 0 ? 3 : 2) + ")",
			()-> TooltipUtil.translate("port.rs_ctr.logic" + (outInv != 0 ? 5 : 4))
		).setSize(0.0625F, 0.0625F).setLocation(0.625F, 0.5F, 0.25F, o);
	}

}
