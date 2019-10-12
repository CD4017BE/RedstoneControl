package cd4017be.rscpl.util;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Used to load and store the internal state of circuits.
 * Basically wraps {@link NBTTagCompound} so I don't need to worry about Minecraft's code obfuscation in my compiler.
 * @author CD4017BE
 */
public class StateBuffer {

	/**
	 * the backing compound tag
	 */
	public final NBTTagCompound nbt;

	/**
	 * initialize with new empty compound tag
	 */
	public StateBuffer() {
		this.nbt = new NBTTagCompound();
	}

	/**
	 * initialize with given compound tag
	 * @param nbt
	 */
	public StateBuffer(NBTTagCompound nbt) {
		this.nbt = nbt;
	}

	public StateBuffer set(String key, byte value) {
		nbt.setByte(key, value);
		return this;
	}

	public StateBuffer set(String key, short value) {
		nbt.setShort(key, value);
		return this;
	}

	public StateBuffer set(String key, int value) {
		nbt.setInteger(key, value);
		return this;
	}

	public StateBuffer set(String key, long value) {
		nbt.setLong(key, value);
		return this;
	}

	public StateBuffer set(String key, float value) {
		nbt.setFloat(key, value);
		return this;
	}

	public StateBuffer set(String key, double value) {
		nbt.setDouble(key, value);
		return this;
	}

	public StateBuffer set(String key, byte[] value) {
		nbt.setByteArray(key, value);
		return this;
	}

	public StateBuffer set(String key, short[] value) {
		byte[] arr = new byte[value.length * 2];
		for (int i = 0, j = 0; i < value.length; i++) {
			short v = value[i];
			arr[j++] = (byte)(v >> 8);
			arr[j++] = (byte)v;
		}
		nbt.setByteArray(key, arr);
		return this;
	}

	public StateBuffer set(String key, int[] value) {
		nbt.setIntArray(key, value);
		return this;
	}

	public byte getByte(String key) {
		return nbt.getByte(key);
	}

	public short getShort(String key) {
		return nbt.getShort(key);
	}

	public int getInt(String key) {
		return nbt.getInteger(key);
	}

	public long getLong(String key) {
		return nbt.getLong(key);
	}

	public float getFloat(String key) {
		return nbt.getFloat(key);
	}

	public double getDouble(String key) {
		return nbt.getDouble(key);
	}

	public void getArr(String key, byte[] arr) {
		byte[] buf = nbt.getByteArray(key);
		int n = Math.min(buf.length, arr.length);
		System.arraycopy(buf, 0, arr, 0, n);
		Arrays.fill(arr, n, arr.length, (byte)0);
	}

	public void getArr(String key, short[] arr) {
		byte[] buf = nbt.getByteArray(key);
		int n = Math.min(buf.length / 2, arr.length);
		for (int i = 0, j = 0; i < n; i++)
			arr[i] = (short)(buf[j++] << 8 | buf[j++] & 0xff);
		Arrays.fill(arr, n, arr.length, (short)0);
	}

	public void getArr(String key, int[] arr) {
		int[] buf = nbt.getIntArray(key);
		int n = Math.min(buf.length, arr.length);
		System.arraycopy(buf, 0, arr, 0, n);
		Arrays.fill(arr, n, arr.length, 0);
	}

}
