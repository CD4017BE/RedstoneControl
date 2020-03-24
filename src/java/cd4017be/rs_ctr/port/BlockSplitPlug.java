package cd4017be.rs_ctr.port;

import java.util.Arrays;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.rs_ctr.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** @author CD4017BE */
public class BlockSplitPlug extends SplitPlug implements BlockHandler {

	BlockHandler[] callbacks;
	BlockReference lastValue;

	public BlockSplitPlug(MountedPort port) {
		super(port);
	}

	@Override
	public int addLinks(int n) {
		n = super.addLinks(n);
		callbacks = callbacks == null ? new BlockHandler[n] : Arrays.copyOf(callbacks, wires.length);
		return n;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		callbacks = new BlockHandler[wires.length];
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if(callback instanceof BlockHandler)
			(callbacks[pin - 1] = (BlockHandler)callback).updateBlock(lastValue);
		else callbacks[pin - 1] = null;
	}

	@Override
	protected WireType type() {
		return WireType.BLOCK;
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(Objects.split_b, wires.length);
	}

	@Override
	public void updateBlock(BlockReference value) {
		if(BlockReference.equal(value, lastValue)) return;
		lastValue = value;
		for(BlockHandler h : callbacks)
			if (h != null)
				h.updateBlock(value);
	}

}
