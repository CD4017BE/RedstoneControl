package cd4017be.rs_ctr.port;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class StatusLamp extends Plug implements SignalHandler, ITESRenderComp {

	public static final String ID = "lamp";

	private int state;

	@Override
	protected String id() {
		return ID;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
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
		if (state > 0) light = light & 0xffff0000 | 0xef;
		PortRenderer.PORT_RENDER.drawModel(buffer, (float)(x + port.pos.x), (float)(y + port.pos.y), (float)(z + port.pos.z), Orientation.fromFacing(port.face), light, state > 0 ? "_plug.main(4)" : "_plug.main(3)");
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return null;
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		return "\n" + state + "\nx" + Integer.toHexString(state);
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(Objects.lamp);
	}

	@Override
	public void onLoad(MountedPort port) {
		super.onLoad(port);
		port.owner.setPortCallback(port.pin, this);
	}

	@Override
	public void onUnload() {
		if (port == null) return;
		port.owner.setPortCallback(port.pin, null);
		this.port = null;
	}

	@Override
	public void updateSignal(int value) {
		state = value;
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
	}

}
