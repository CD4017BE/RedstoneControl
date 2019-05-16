package cd4017be.rs_ctr.signal;

import java.util.List;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class Constant implements IConnector, IBlockRenderComp {

	public static final String ID = "const";
	private MountedSignalPort port;
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
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), "_plug.main(1)");
	}

	@Override
	public void onRemoved(MountedSignalPort port, EntityPlayer player) {
		ItemStack stack = new ItemStack(Objects.constant);
		stack.setTagCompound(serializeNBT());
		if (player == null) ItemFluidUtil.dropStack(stack, port.getWorld(), port.getPos());
		else if (!player.isCreative()) ItemFluidUtil.dropStack(stack, player);
		port.owner.getPortCallback(port.pin).accept(0);
	}

	@Override
	public String displayInfo(MountedSignalPort port, int linkID) {
		return "\n" + value;
	}

	@Override
	public void onLoad(MountedSignalPort port) {
		this.port = port;
		port.owner.getPortCallback(port.pin).accept(value);
	}

}
