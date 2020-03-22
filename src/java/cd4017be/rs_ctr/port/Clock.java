package cd4017be.rs_ctr.port;

import java.util.List;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.ITickReceiver;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class Clock extends Plug implements ITickReceiver, IBlockRenderComp {

	public static final String ID = "clock";

	long phase;
	int interval;
	WorldInfo worldRef;
	SignalHandler callback;

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("int", interval);
		nbt.setInteger("pha", (int)(phase % (long)(interval << 1)));
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		interval = Math.max(1, nbt.getInteger("int"));
		phase = nbt.getInteger("pha");
	}

	@Override
	protected String id() {
		return ID;
	}

	@Override
	protected ItemStack drop() {
		ItemStack stack = new ItemStack(Objects.clock);
		stack.setTagCompound(serializeNBT());
		return stack;
	}

	@Override
	public boolean tick() {
		if (worldRef == null) return false;
		int t = (int)(worldRef.getWorldTotalTime() - phase);
		if (t == 0) {
			callback.updateSignal(65535);
		} else if (t >= interval) {
			phase += interval << 1;
			callback.updateSignal(0);
		}
		return true;
	}

	@Override
	public void onLoad(MountedPort port) {
		super.onLoad(port);
		if (worldRef == null) TickRegistry.instance.add(this);
		worldRef = port.getWorld().getWorldInfo();
		callback = (SignalHandler)port.owner.getPortCallback(port.pin);
		long t = Math.floorMod(worldRef.getWorldTotalTime() - phase + interval, interval << 1) - interval;
		phase = worldRef.getWorldTotalTime() - t;
		callback.updateSignal(t >= 0 ? 65535 : 0);
	}

	@Override
	public void onUnload() {
		super.onUnload();
		worldRef = null;
		callback = null;
	}

	@Override
	public void onRemoved(MountedPort port, EntityPlayer player) {
		super.onRemoved(port, player);
		port.owner.onPortModified(port, IPortProvider.E_DISCONNECT);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), "_plug.misc(3)");
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		return "\n" + TooltipUtil.format("port.rs_ctr.clock", (float)interval / 20F);
	}

}
