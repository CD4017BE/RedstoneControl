package cd4017be.rs_ctr.port;

import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.item.ItemConstantPlug;
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
public class Constant extends Plug implements IBlockRenderComp {

	public static final String ID = "const";
	public int value;
	public byte dsp;

	@Override
	protected String id() {
		return ID;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("val", value);
		nbt.setByte("dsp", dsp);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		value = nbt.getInteger("val");
		dsp = nbt.getByte("dsp");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), "_plug.misc(2)");
	}

	@Override
	public void onRemoved(MountedPort port, EntityPlayer player) {
		super.onRemoved(port, player);
		port.owner.onPortModified(port, IPortProvider.E_DISCONNECT);
	}

	@Override
	protected ItemStack drop() {
		ItemStack stack = new ItemStack(Objects.constant);
		stack.setTagCompound(serializeNBT());
		return stack;
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		return "\n" + ItemConstantPlug.toString(value, dsp);
	}

	@Override
	public void onLoad(MountedPort port) {
		super.onLoad(port);
		((SignalHandler)port.owner.getPortCallback(port.pin)).updateSignal(value);
	}

}
