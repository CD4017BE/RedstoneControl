package cd4017be.rs_ctr.signal;

import java.util.function.IntConsumer;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class StatusLamp implements IConnector, IntConsumer, ITESRenderComp {

	public static final String ID = "lamp";

	private MountedSignalPort port;
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
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (state > 0) light |= 0xef;
		PortRenderer.PORT_RENDER.drawModel(buffer, (float)(x + port.pos.x), (float)(y + port.pos.y), (float)(z + port.pos.z), Orientation.fromFacing(port.face), light, state > 0 ? "_plug.main(4)" : "_plug.main(3)");
	}

	@Override
	public String displayInfo(MountedSignalPort port, int linkID) {
		return "\n" + state;
	}

	@Override
	public void onRemoved(MountedSignalPort port, EntityPlayer player) {
		ItemStack stack = new ItemStack(Objects.lamp);
		if (player == null) ItemFluidUtil.dropStack(stack, port.getWorld(), port.getPos());
		else if (!player.isCreative()) ItemFluidUtil.dropStack(stack, player);
	}

	@Override
	public void setPort(MountedSignalPort port) {
		this.port = port;
	}

	@Override
	public void onLoad(MountedSignalPort port) {
		IConnector.super.onLoad(port);
		port.owner.setPortCallback(port.pin, this);
	}

	@Override
	public void onUnload() {
		if (port == null) return;
		port.owner.setPortCallback(port.pin, null);
		this.port = null;
	}

	@Override
	public void accept(int value) {
		state = value;
		port.owner.onPortModified(port, ISignalIO.E_CON_UPDATE);
	}

}
