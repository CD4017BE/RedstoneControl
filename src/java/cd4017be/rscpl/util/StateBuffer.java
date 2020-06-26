package cd4017be.rscpl.util;

import java.util.Arrays;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.Constants.NBT;

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
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < value.length; i++)
			list.appendTag(new NBTTagShort(value[i]));
		nbt.setTag(key, list);
		return this;
	}

	public StateBuffer set(String key, int[] value) {
		nbt.setIntArray(key, value);
		return this;
	}

	public StateBuffer set(String key, float[] value) {
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < value.length; i++)
			list.appendTag(new NBTTagFloat(value[i]));
		nbt.setTag(key, list);
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
		int n = arr.length;
		if (nbt.hasKey(key, NBT.TAG_INT_ARRAY)) {//backwards compatibility
			byte[] buf = nbt.getByteArray(key);
			n = Math.min(buf.length / 2, n);
			for (int i = 0, j = 0; i < n; i++)
				arr[i] = (short)(buf[j++] << 8 | buf[j++] & 0xff);
		} else {
			NBTTagList list = nbt.getTagList(key, NBT.TAG_SHORT);
			n = Math.min(list.tagCount(), n);
			for (int i = 0; i < n; i++)
				arr[i] = (short)list.getIntAt(i);
		}
		Arrays.fill(arr, n, arr.length, (short)0);
	}

	public void getArr(String key, int[] arr) {
		int[] buf = nbt.getIntArray(key);
		int n = Math.min(buf.length, arr.length);
		System.arraycopy(buf, 0, arr, 0, n);
		Arrays.fill(arr, n, arr.length, 0);
	}

	public void getArr(String key, float[] arr) {
		NBTTagList list = nbt.getTagList(key, NBT.TAG_FLOAT);
		int n = Math.min(list.tagCount(), arr.length);
		for (int i = 0; i < n; i++)
			arr[i] = list.getFloatAt(i);
		Arrays.fill(arr, n, arr.length, 0F);
	}

}
