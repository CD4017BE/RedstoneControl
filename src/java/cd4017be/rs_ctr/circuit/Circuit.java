package cd4017be.rs_ctr.circuit;

import java.util.Arrays;
import java.util.UUID;
import cd4017be.rscpl.util.IStateSerializable;
import cd4017be.rscpl.util.StateBuffer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * parent template class for all ASM compiled circuit implementations.
 * @author CD4017BE
 */
public abstract class Circuit implements INBTSerializable<NBTTagCompound>, IStateSerializable {

	/**the circuit "serial number" */
	protected UUID ID;
	/**bitmap of which input pins trigger interrupt */
	protected int interruptPins;
	/**IO buffers */
	public int[] inputs, outputs;

	/**
	 * The main update routine
	 * @return 1 when internal circuit state changed {@code | 2 << i} for each output i that changed
	 */
	public abstract int tick();

	/**
	 * @param pin input pin index
	 * @return whether the given input should trigger an interrupt
	 */
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

	/**
	 * @return the actual implemented instance of this circuit (if it isn't already)
	 * @see UnloadedCircuit
	 */
	public Circuit load() {
		return this;
	}

}
