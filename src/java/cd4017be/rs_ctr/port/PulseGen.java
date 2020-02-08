package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.IIntegratedConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** @author CD4017BE */
public class PulseGen extends WireBranch implements IIntegratedConnector, SignalHandler, IUpdatable {

	public static final String ID = "pulse_gen";
	private static final byte S_OFF = 0, S_RISE = 1, S_ON = 2, S_FALL = 3;

	SignalHandler out = SignalHandler.NOP;
	int last;
	byte state, tick;

	public PulseGen() {
		super(WireType.SIGNAL);
		subPort(new Port(this, 0, SignalHandler.class, true));
	}

	@Override
	public void updateSignal(int value) {
		if(value == last) return;
		last = value;
		if(tick == 0) {
			tick = TickRegistry.TICK;
			TickRegistry.schedule(this);
			state = S_RISE;
		} else if(tick != TickRegistry.TICK) tick = TickRegistry.TICK;
		else if(state == S_FALL) state = S_ON;
	}

	@Override
	public void process() {
		boolean repulse = tick == TickRegistry.TICK;
		switch(state) {
		case S_RISE:
			if(repulse) state = S_ON;
			else {
				state = S_FALL;
				tick = TickRegistry.TICK;
			}
			TickRegistry.schedule(this);
			out.updateSignal(65535);
			break;
		case S_ON:
			if(!repulse) {
				state = S_FALL;
				tick = TickRegistry.TICK;
			}
			TickRegistry.schedule(this);
			break;
		case S_FALL:
			if(repulse) {
				state = S_RISE;
				TickRegistry.schedule(this);
			} else {
				state = S_OFF;
				tick = 0;
			}
			out.updateSignal(0);
			break;
		default: //this case should never happen
			state = S_OFF;
			tick = 0;
		}
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : SignalHandler.NOP;
		out.updateSignal(state >= S_ON ? 65535 : 0);
	}

	@Override
	protected String id() {
		return ID;
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(Objects.pulse_gen);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("last", last);
		nbt.setByte("state", state);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		last = nbt.getInteger("last");
		state = nbt.getByte("state");
	}

	@Override
	public void onLoad(MountedPort port) {
		super.onLoad(port);
		port.owner.setPortCallback(port.pin, this);
		if(state != S_OFF && tick == 0) {
			tick = TickRegistry.TICK;
			TickRegistry.schedule(this);
		}
	}

	@Override
	public void onUnload() {
		super.onUnload();
		port.owner.setPortCallback(port.pin, null);
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		return TooltipUtil.translate("port.rs_ctr.pulse") + super.displayInfo(port, subPort.getLink());
	}

	@Override
	public void render(List<BakedQuad> quads) {
		super.render(quads);
		PortRenderer.PORT_RENDER.drawModel(
			quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z,
			Orientation.fromFacing(port.face), "_plug.main(8)"
		);
	}

}
