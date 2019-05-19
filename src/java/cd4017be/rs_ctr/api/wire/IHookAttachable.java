package cd4017be.rs_ctr.api.wire;

import java.util.Map.Entry;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author CD4017BE
 *
 */
public interface IHookAttachable extends ISignalIO {

	Int2ObjectMap<RelayPort> getHookPins();

	public static int getAttachmentPos(Vec3d target, EntityPlayer player) {
		target = target.subtract(player.getLook(1).scale(0.0625));
		int p = (int)Math.floor(target.x * 4.0) & 0xf;
		p |= (int)Math.floor(target.y * 4.0) << 4 & 0xf0;
		p |= (int)Math.floor(target.z * 4.0) << 8 & 0xf00;
		return p;
	}

	default int applyOrientation(int pin) {
		Orientation o = getOrientation();
		if (o == Orientation.N) return pin;
		Vec3d vec = o.invRotate(new Vec3d(pin & 3, pin >> 4 & 3, pin >> 8 & 3).addVector(-1.5, -1.5, -1.5));
		return (int)(vec.x + 1.5) | (int)(vec.y + 1.5) << 4 | (int)(vec.z + 1.5) << 8;
	}

	default boolean doAttachHook(int pin) {
		if ((pin & 0x333) != pin) return false;
		Int2ObjectMap<RelayPort> pins = getHookPins();
		if (pins.containsKey(0x8000 | pin)) return false;
		RelayPort port = new RelayPort(this, pin);
		pins.put(port.pin, port);
		port.orient(getOrientation());
		port = port.opposite;
		pins.put(port.pin, port);
		port.orient(getOrientation());
		onPortModified(port, E_HOOK_ADD);
		return true;
	}

	default boolean removeHook(int pin, EntityPlayer player) {
		Int2ObjectMap<RelayPort> pins = getHookPins();
		RelayPort port;
		World world = null; BlockPos pos = null;
		if ((port = pins.remove(pin & 0xfff | 0x8000)) != null) {
			port.setConnector(null, player);
			world = port.getWorld();
			pos = port.getPos();
		}
		if ((port = pins.remove(pin & 0xfff | 0x9000)) != null) {
			port.setConnector(null, player);
			world = port.getWorld();
			pos = port.getPos();
		}
		if (world == null || pos == null) return false;
		if (player == null)
			ItemFluidUtil.dropStack(new ItemStack(RelayPort.HOOK_ITEM), world, pos);
		else if (!player.isCreative())
			ItemFluidUtil.dropStack(new ItemStack(RelayPort.HOOK_ITEM), player);
		onPortModified(null, E_HOOK_REM);
		return true;
	}

	default NBTTagCompound storeHooks() {
		Int2ObjectMap<RelayPort> pins = getHookPins();
		if (pins.isEmpty()) return null;
		NBTTagCompound nbt = new NBTTagCompound();
		for (Entry<Integer, RelayPort> e : pins.entrySet())
			nbt.setTag(Integer.toHexString(e.getKey()), e.getValue().serializeNBT());
		return nbt;
	}

	default void loadHooks(NBTTagCompound nbt) {
		Int2ObjectMap<RelayPort> pins = getHookPins();
		pins.clear();
		Orientation o = getOrientation();
		for (String key : nbt.getKeySet())
			try {
				int pin = Integer.parseInt(key, 16);
				if (pin != (pin & 0x9333)) continue;
				RelayPort port = pins.get(pin);
				if (port == null) {
					port = new RelayPort(this, pin);
					pins.put(port.pin, port);
					pins.put(port.opposite.pin, port.opposite);
					if ((pin & 0x1000) != 0) port = port.opposite;
				}
				port.deserializeNBT(nbt.getCompoundTag(key));
				port.orient(o);
			} catch (NumberFormatException e) {}
	}

	default Orientation getOrientation() {
		return Orientation.N;
	}

	public static final int E_HOOK_ADD = 256, E_HOOK_REM = 512;

}
