package cd4017be.rs_ctr.api.signal;

import java.util.HashMap;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Stores information about a connection between MountedSignalPorts and is mainly used client side to render the connection.<dl>
 * Implementations are registered via {@link #REGISTRY} using a unique String ID which must be written to tag name "id" when implementing {@link INBTSerializable#serializeNBT()}.
 * @author CD4017BE
 */
public interface IConnector extends INBTSerializable<NBTTagCompound> {

	/**map of registered Connector types */
	public static final HashMap<String, Class<?extends IConnector>> REGISTRY = new HashMap<>();

	/**
	 * @param port the port holding this connector
	 * @param linkID current signal link
	 * @return the additional tool-tip shown when the port is aimed.
	 */
	default String displayInfo(MountedSignalPort port, int linkID) {
		return linkID != 0 ? "\nID " + linkID : "";
	}

	/**
	 * Perform special removal actions like dropping items and/or calling {@link SignalPort#disconnect()}.
	 * @param port
	 * @param player
	 */
	void onRemoved(MountedSignalPort port, @Nullable EntityPlayer player);

	/**
	 * called when the given port is loaded into the world.
	 * @param port the port holding this connector.
	 */
	default void onLoad(MountedSignalPort port) {}

	/**
	 * called when the port holding this connector is unloaded.
	 */
	default void onUnload() {}

	/**
	 * @param nbt serialized data
	 * @return a deserialized connector instance or null if data invalid.
	 */
	public static IConnector load(NBTTagCompound nbt) {
		Class<?extends IConnector> c = REGISTRY.get(nbt.getString("id"));
		if (c == null) return null;
		try {
			IConnector con = c.newInstance();
			con.deserializeNBT(nbt);
			return con;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * implemented by {@link Item}s that want to interact with {@link MountedSignalPort}s.
	 * @author cd4017be
	 */
	public interface IConnectorItem {

		/**
		 * Perform attachment of given connector item on given SignalPort by calling {@link MountedSignalPort#setConnector(IConnector, EntityPlayer)} and eventually {@link SignalPort#connect(SignalPort)}.
		 * @param stack the itemstack used
		 * @param port the port to interact with
		 * @param player the interacting player
		 */
		void doAttach(ItemStack stack, MountedSignalPort port, EntityPlayer player);

	}

}
