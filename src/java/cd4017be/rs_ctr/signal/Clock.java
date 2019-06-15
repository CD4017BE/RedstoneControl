package cd4017be.rs_ctr.signal;

import java.util.List;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.ITickReceiver;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.com.SignalHandler;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldInfo;


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
	public void onLoad(MountedSignalPort port) {
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
	public void onRemoved(MountedSignalPort port, EntityPlayer player) {
		super.onRemoved(port, player);
		port.owner.onPortModified(port, ISignalIO.E_DISCONNECT);
	}

	@Override
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), "_plug.main(6)");
	}

	@Override
	public String displayInfo(MountedSignalPort port, int linkID) {
		return "\n" + TooltipUtil.format("port.rs_ctr.clock", (float)interval / 20F);
	}

}
