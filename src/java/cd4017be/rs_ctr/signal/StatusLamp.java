package cd4017be.rs_ctr.signal;

import java.util.function.IntConsumer;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
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
public class StatusLamp implements IConnector, IntConsumer {

	public static final String ID = "lamp";

	private MountedSignalPort link;
	private int state;

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("id", ID);
		nbt.setInteger("state", state);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		state = nbt.getInteger("state");
	}

	@Override
	public void renderConnection(World world, BlockPos pos, MountedSignalPort port, double x, double y, double z, BufferBuilder buffer) {
		WireRenderer.instance.drawPlug(buffer, port, (float)x, (float)y, (float)z, world.getCombinedLight(pos, state > 0 ? 15 : 0), state > 0 ? 4 : 3);
	}

	@Override
	public AxisAlignedBB renderSize(World world, BlockPos pos, MountedSignalPort port) {
		return null;
	}

	@Override
	public String displayInfo(MountedSignalPort port) {
		return "\n" + state;
	}

	@Override
	public void onRemoved(MountedSignalPort port, EntityPlayer player) {
		ItemStack stack = new ItemStack(Objects.lamp);
		if (player != null) ItemFluidUtil.dropStack(stack, player);
		else ItemFluidUtil.dropStack(stack, port.getWorld(), port.getPos());
	}

	@Override
	public void onLoad(MountedSignalPort port) {
		this.link = port;
		port.owner.setPortCallback(port.pin, this);
	}

	@Override
	public void onUnload() {
		if (link == null) return;
		link.owner.setPortCallback(link.pin, null);
		this.link = null;
	}

	@Override
	public void accept(int value) {
		state = value;
		link.owner.onPortModified(link, ISignalIO.E_CON_UPDATE);
	}

}
