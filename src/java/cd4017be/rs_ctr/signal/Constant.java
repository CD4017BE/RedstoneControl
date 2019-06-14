package cd4017be.rs_ctr.signal;

import java.util.List;
import java.util.function.IntConsumer;

import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.signal.ISignalIO;
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
public class Constant extends Plug implements IBlockRenderComp {

	public static final String ID = "const";
	public int value;

	@Override
	protected String id() {
		return ID;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
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
		super.onRemoved(port, player);
		port.owner.onPortModified(port, ISignalIO.E_DISCONNECT);
	}

	@Override
	protected ItemStack drop() {
		ItemStack stack = new ItemStack(Objects.constant);
		stack.setTagCompound(serializeNBT());
		return stack;
	}

	@Override
	public String displayInfo(MountedSignalPort port, int linkID) {
		return "\n" + value;
	}

	@Override
	public void onLoad(MountedSignalPort port) {
		super.onLoad(port);
		((IntConsumer)port.owner.getPortCallback(port.pin)).accept(value);
	}

}
