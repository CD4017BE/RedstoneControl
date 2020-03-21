package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * 
 * @author CD4017BE
 */
public class BlockSplitter extends WallMountGate implements BlockHandler {

	protected final BlockHandler[] callbacks = new BlockHandler[4];
	protected BlockReference state;

	{
		ports = new MountedPort[5];
		for (int i = 0; i < 4; i++)
			ports[i] = new MountedPort(this, i, BlockHandler.class, true).setLocation(0.75F, 0.125F + i * 0.25F, 0.125F, EnumFacing.EAST).setName("port.rs_ctr.bo");
		ports[4] = new MountedPort(this, 4, BlockHandler.class, false).setLocation(0.25F, 0.5F, 0.125F, EnumFacing.WEST).setName("port.rs_ctr.bi");
	}

	@Override
	public BlockHandler getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		BlockHandler c = callback instanceof BlockHandler ? (BlockHandler)callback : null;
		callbacks[pin] = c;
		if (c != null) c.updateBlock(state);
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).updateBlock(null);
	}

	@Override
	public void updateBlock(BlockReference ref) {
		if (BlockReference.equal(state, ref)) return;
		state = ref;
		for (BlockHandler c : callbacks)
			if (c != null)
				c.updateBlock(ref);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE)
			if (state != null)
				nbt.setTag("state", state.serializeNBT());
			else nbt.removeTag("state");
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE)
			if (nbt.hasKey("state", NBT.TAG_COMPOUND))
				state = new BlockReference(nbt.getCompoundTag("state"));
			else state = null;
		super.loadState(nbt, mode);
	}

	@Override
	public Object getState(int id) {
		return state;
	}

}
