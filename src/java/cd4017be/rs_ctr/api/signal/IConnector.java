package cd4017be.rs_ctr.api.signal;

import java.util.HashMap;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Stores information about a connection between MountedSignalPorts and is mainly used client side to render the connection.<dl>
 * Implementations are registered via {@link #REGISTRY} using a unique String ID which must be written to tag name "id" when implementing {@link INBTSerializable#serializeNBT()}.
 * @author CD4017BE
 */
public interface IConnector extends INBTSerializable<NBTTagCompound> {

	public static final HashMap<String, Class<?extends IConnector>> REGISTRY = new HashMap<>();

	default String displayInfo(MountedSignalPort port) {
		return "";
	}

	@SideOnly(Side.CLIENT)
	void renderConnection(World world, BlockPos pos, MountedSignalPort port, double x, double y, double z, BufferBuilder buffer);

	AxisAlignedBB renderSize(World world, BlockPos pos, MountedSignalPort port);

	/**
	 * Perform special removal actions like dropping items and/or calling {@link SignalPort#disconnect()}.
	 * @param port
	 * @param player
	 */
	void onRemoved(MountedSignalPort port, @Nullable EntityPlayer player);

	default void onLoad(MountedSignalPort port) {}

	default void onUnload() {}

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

	public interface IConnectorItem {

		/**
		 * Perform attachment of given connector item on given SignalPort by calling {@link MountedSignalPort#setConnector(IConnector, EntityPlayer)} and eventually {@link SignalPort#connect(SignalPort)}.
		 * @param stack
		 * @param port
		 * @param player
		 */
		void doAttach(ItemStack stack, MountedSignalPort port, EntityPlayer player);

	}

}
