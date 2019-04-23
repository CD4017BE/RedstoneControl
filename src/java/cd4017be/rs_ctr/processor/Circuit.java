package cd4017be.rs_ctr.processor;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.IntConsumer;

import cd4017be.rs_ctr.api.signal.SignalReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * parent template class for all ASM compiled circuit implementations.
 * @author CD4017BE
 */
public abstract class Circuit implements INBTSerializable<NBTTagCompound> {

	protected UUID ID;
	protected int interruptPins;
	public int[] inputs, outputs;
	public IntConsumer[] callbacks;

	public abstract boolean tick();
	public abstract void setState(StateBuffer state);
	public abstract StateBuffer getState();

	public boolean isInterrupt(int pin) {
		return (interruptPins >> pin & 1) != 0;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("intpin", (byte)interruptPins);
		nbt.setIntArray("in", inputs);
		nbt.setIntArray("out", outputs);
		nbt.setTag("state", getState().nbt);
		nbt.setLong("IDm", ID.getMostSignificantBits());
		nbt.setLong("IDl", ID.getLeastSignificantBits());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		interruptPins = nbt.getByte("intpin") & 0xff;
		inputs = nbt.getIntArray("in");
		outputs = nbt.getIntArray("out");
		Arrays.fill(callbacks = new IntConsumer[outputs.length], SignalReceiver.NOP);
		setState(new StateBuffer(nbt.getCompoundTag("state")));
		ID = new UUID(nbt.getLong("IDm"), nbt.getLong("IDl"));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Serial-ID = ").append(ID);
		sb.append("\nInput signals = ").append(Arrays.toString(inputs));
		sb.append("\nOutput signals = ").append(Arrays.toString(outputs));
		NBTTagCompound nbt;
		try {nbt = getState().nbt;}
		catch(Exception e) {nbt = null;}
		sb.append("\nMemory states = ").append(nbt);
		return sb.toString();
	}

	public Circuit loadCode() {
		return this;
	}

}
