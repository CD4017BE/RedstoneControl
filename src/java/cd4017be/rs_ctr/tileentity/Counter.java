package cd4017be.rs_ctr.tileentity;

import java.util.List;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.gui.BlockButton;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/** @author CD4017BE */
public class Counter extends WallMountGate implements SignalHandler {

	SignalHandler outC = SignalHandler.NOP, outR = SignalHandler.NOP;
	int clkI, clkO, limit, count;
	boolean dir = true;
	BlockButton button = new BlockButton(
		(a) -> {
			dir = !dir;
			markDirty(REDRAW);
		},
		() -> (dir ? "_buttons.num(0)" : "_buttons.num(1)"),
		() -> TooltipUtil.translate(dir ? "port.rs_ctr.inc" : "port.rs_ctr.dec")
	).setSize(0.0625F, 0.0625F);

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, SignalHandler.class, false)
			.setLocation(0.25F, 0.875F, 0.125F, EnumFacing.WEST)
			.setName("port.rs_ctr.clk"),
			new MountedPort(this, 1, SignalHandler.class, false)
			.setLocation(0.25F, 0.125F, 0.125F, EnumFacing.WEST)
			.setName("port.rs_ctr.limit"),
			new MountedPort(this, 2, SignalHandler.class, true)
			.setLocation(0.75F, 0.125F, 0.125F, EnumFacing.EAST)
			.setName("port.rs_ctr.clko"),
			new MountedPort(this, 3, SignalHandler.class, true)
			.setLocation(0.75F, 0.875F, 0.125F, EnumFacing.EAST)
			.setName("port.rs_ctr.count")
		};
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		return pin == 0 ? this : (v) -> limit = v;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		SignalHandler h = callback instanceof SignalHandler
			? (SignalHandler)callback : SignalHandler.NOP;
		if(pin == 2) outR = h;
		else outC = h;
	}

	@Override
	protected void resetPin(int pin) {
		if(pin == 1) limit = 0;
	}

	@Override
	public void updateSignal(int value) {
		if(value == clkI) return;
		clkI = value;
		int c;
		if(dir) {
			c = count + 1;
			if(c > 0 && c >= limit) {
				c = Math.min(0, limit + 1);
				outR.updateSignal(clkO ^= 0xffff);
			}
		} else {
			c = count - 1;
			if(c < 0 && c <= limit) {
				c = Math.max(0, limit - 1);
				outR.updateSignal(clkO ^= 0xffff);
			}
		}
		outC.updateSignal(count = c);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if(mode == SAVE) {
			nbt.setInteger("clkI", clkI);
			nbt.setInteger("clkO", clkO);
			nbt.setInteger("lim", limit);
			nbt.setInteger("cnt", count);
		}
		if(mode <= SYNC)
			nbt.setBoolean("dir", dir);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if(mode == SAVE) {
			clkI = nbt.getInteger("clkI");
			clkO = nbt.getInteger("clkO");
			limit = nbt.getInteger("lim");
			count = nbt.getInteger("cnt");
		}
		if(mode <= SYNC)
			dir = nbt.getBoolean("dir");
		super.loadState(nbt, mode);
	}

	@Override
	protected void orient(Orientation o) {
		button.setLocation(0.375F, 0.875F, 0.25F, o);
		super.orient(o);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(button);
	}

}
