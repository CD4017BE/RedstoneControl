package cd4017be.rs_ctr.signal;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


/**
 * @author CD4017BE
 *
 */
public class Constant implements IConnector {

	public static final String ID = "const";
	public int value;

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("id", ID);
		nbt.setInteger("val", value);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		value = nbt.getInteger("val");
	}

	@Override
	public void renderConnection(World world, BlockPos pos, MountedSignalPort port, double x, double y, double z, BufferBuilder buffer) {
		WireRenderer.instance.drawPlug(buffer, port, (float)x, (float)y, (float)z, world.getCombinedLight(pos, 0), 1);
	}

	@Override
	public AxisAlignedBB renderSize(World world, BlockPos pos, MountedSignalPort port) {
		return null;
	}

	@Override
	public void onRemoved(MountedSignalPort port, EntityPlayer player) {
		ItemStack stack = new ItemStack(Objects.constant);
		stack.setTagCompound(serializeNBT());
		if (player != null) ItemFluidUtil.dropStack(stack, player);
		else ItemFluidUtil.dropStack(stack, port.getWorld(), port.getPos());
		port.owner.getPortCallback(port.pin).accept(0);
	}

	@Override
	public String displayInfo(MountedSignalPort port) {
		return "\n" + value;
	}

}
