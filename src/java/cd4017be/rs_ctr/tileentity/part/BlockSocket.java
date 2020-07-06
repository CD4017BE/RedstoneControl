package cd4017be.rs_ctr.tileentity.part;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.rs_ctr.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;


/** 
 * @author CD4017BE */
public class BlockSocket extends Socket implements BlockHandler {

	public static final String ID = "socket_b";

	BlockHandler callback = BlockHandler.NOP;
	BlockReference state;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		if (state != null) nbt.setTag("state", state.serializeNBT());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		if (nbt.hasKey("state", NBT.TAG_COMPOUND))
			state = new BlockReference(nbt.getCompoundTag("state"));
		else state = null;
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.socket_b);
	}

	@Override
	public Object getPortCallback() {
		return this;
	}

	@Override
	public void setPortCallback(Object callback) {
		(this.callback = callback instanceof BlockHandler ? (BlockHandler)callback : BlockHandler.NOP)
		.updateBlock(state);
	}

	@Override
	public void resetInput() {
		callback.updateBlock(null);
	}

	@Override
	public void updateBlock(BlockReference value) {
		callback.updateBlock(state = value);
	}

	@Override
	protected String portLabel(boolean out) {
		return out ? "port.rs_ctr.bo" : "port.rs_ctr.bi";
	}

	@Override
	protected Class<?> type() {
		return BlockHandler.class;
	}

	@Override
	public Object getState(int id) {
		return state;
	}

}
