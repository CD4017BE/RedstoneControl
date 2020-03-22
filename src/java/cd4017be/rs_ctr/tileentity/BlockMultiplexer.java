package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;

/** @author CD4017BE */
public class BlockMultiplexer extends WallMountGate
implements SignalHandler, IUpdatable {

	final BlockReference[] in = new BlockReference[4];
	BlockHandler out;
	int sel;
	byte tick;
	boolean delayed;
	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setName("port.rs_ctr.bi0").setLocation(0.25, 0.125, 0.125, EnumFacing.WEST),
			new MountedPort(this, 1, BlockHandler.class, false).setName("port.rs_ctr.bi1").setLocation(0.25, 0.375, 0.125, EnumFacing.WEST),
			new MountedPort(this, 2, BlockHandler.class, false).setName("port.rs_ctr.bi2").setLocation(0.25, 0.625, 0.125, EnumFacing.WEST),
			new MountedPort(this, 3, BlockHandler.class, false).setName("port.rs_ctr.bi3").setLocation(0.25, 0.875, 0.125, EnumFacing.WEST),
			new MountedPort(this, 4, SignalHandler.class, false).setName("port.rs_ctr.sel").setLocation(0.75, 0.875, 0.125, EnumFacing.EAST),
			new MountedPort(this, 5, BlockHandler.class, true).setName("port.rs_ctr.bo").setLocation(0.75, 0.125, 0.125, EnumFacing.EAST)
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		if(pin < 4)
			return (BlockHandler)(ref) -> {
				if(BlockReference.equalDelayed(ref, in[pin], 1)) return;
				scheduleUpdate();
				in[pin] = BlockReference.delayed(ref, 1);
			};
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		out = callback instanceof BlockHandler ? (BlockHandler)callback : null;
		if(out != null)
			out.updateBlock(sel >= 0 ? in[sel] : null);
	}

	@Override
	protected void resetPin(int pin) {
		Object o = getPortCallback(pin);
		if(pin < 4)
			((BlockHandler)o).updateBlock(null);
		else updateSignal(0);
	}

	@Override
	public void updateSignal(int value) {
		if(value < 0 || value >= 4) value = -1;
		if(value == sel) return;
		scheduleUpdate();
		sel = value;
	}

	private void scheduleUpdate() {
		if(tick == 0) {
			tick = TickRegistry.TICK;
			TickRegistry.schedule(this);
		} else if(tick != TickRegistry.TICK) {
			tick = TickRegistry.TICK;
			if(out != null)
				out.updateBlock(sel >= 0 ? in[sel] : null);
			delayed = true;
		}
	}

	@Override
	public void process() {
		if(delayed) {
			delayed = false;
			TickRegistry.schedule(this);
			return;
		}
		tick = 0;
		if(out != null)
			out.updateBlock(sel >= 0 ? in[sel] : null);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if(mode == SAVE) {
			nbt.setInteger("sel", sel);
			for(int i = 0; i < in.length; i++) {
				String key = "in" + i;
				BlockReference ref = in[i];
				if(ref != null)
					nbt.setTag(key, ref.serializeNBT());
				else nbt.removeTag(key);
			}
		}
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if(mode == SAVE) {
			sel = nbt.getInteger("sel");
			for(int i = 0; i < in.length; i++) {
				String key = "in" + i;
				in[i] = nbt.hasKey(key, NBT.TAG_COMPOUND) ?
					new BlockReference(nbt.getCompoundTag(key)) : null;
			}
		}
	}

	@Override
	public Object getState(int id) {
		return id < 4 ? in[id] : id == 4 ? sel : sel < 0 ? null : in[sel];
	}

}
