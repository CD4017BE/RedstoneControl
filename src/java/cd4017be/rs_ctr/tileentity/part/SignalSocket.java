package cd4017be.rs_ctr.tileentity.part;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.rs_ctr.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


/** 
 * @author CD4017BE */
public class SignalSocket extends Socket implements SignalHandler {

	public static final String ID = "socket_s";

	SignalHandler callback = SignalHandler.NOP;
	int state;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("state", state);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		state = nbt.getInteger("state");
	}

	@Override
	public ItemStack onRemove() {
		return new ItemStack(Objects.socket_s);
	}

	@Override
	public Object getPortCallback() {
		return this;
	}

	@Override
	public void setPortCallback(Object callback) {
		(this.callback = callback instanceof SignalHandler ? (SignalHandler)callback : SignalHandler.NOP)
		.updateSignal(state);
	}

	@Override
	public void resetInput() {
		callback.updateSignal(0);
	}

	@Override
	public void updateSignal(int value) {
		callback.updateSignal(state = value);
	}

	@Override
	protected String portLabel(boolean out) {
		return out ? "port.rs_ctr.o" : "port.rs_ctr.i";
	}

	@Override
	protected Class<?> type() {
		return SignalHandler.class;
	}

	@Override
	public Object getState(int id) {
		return state;
	}

}
