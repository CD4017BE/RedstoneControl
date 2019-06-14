package cd4017be.rs_ctr.signal;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 
 * @author cd4017be
 */
public abstract class Plug implements IConnector {

	protected MountedSignalPort port;

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("id", id());
		return nbt;
	}

	@Override
	public void onRemoved(MountedSignalPort port, EntityPlayer player) {
		ItemStack stack = drop();
		if (player == null) ItemFluidUtil.dropStack(stack, port.getWorld(), port.getPos());
		else if (!player.isCreative()) ItemFluidUtil.dropStack(stack, player);
	}

	@Override
	public void setPort(MountedSignalPort port) {
		this.port = port;
	}

	protected abstract String id();

	protected abstract ItemStack drop();

}
