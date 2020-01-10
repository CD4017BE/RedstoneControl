package cd4017be.rs_ctr.tileentity;

import java.util.Map;
import java.util.Map.Entry;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import static cd4017be.rs_ctr.Objects.oc_adapter;
import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.common.Optional.Method;

/** @author CD4017BE */
@InterfaceList(
	value = {
		@Interface(iface = "li.cil.oc.api.network.Environment", modid = "opencomputers"),
		@Interface(iface = "net.minecraft.util.ITickable", modid = "opencomputers"),
		@Interface(iface = "cd4017be.api.rs_ctr.com.EnergyHandler", modid = "opencomputers")
	}
)
public class OC_Adapter extends WallMountGate implements Environment, ITickable, IUpdatable, EnergyHandler {

	public static double OC_UNIT = 1000D;

	private Object node = ComputerAPI.newOCnode(this, "rsio", true);
	private final SignalHandler[] out = new SignalHandler[8];
	private final int[] state = new int[16];
	private int update, interrupt;
	private String target = "";

	{
		ports = new MountedPort[20];
		for (int i = 0; i < 16; i++) {
			ports[i] = new MountedPort(this, i, SignalHandler.class, i < 8)
			.setLocation(.125 + .25 * (i & 3), .125 + .25 * (i >> 2), .75, EnumFacing.SOUTH)
			.setName((i < 8 ? "port.rs_ctr.o" : "port.rs_ctr.i") + (i & 7));
		}
		for (int i = 16; i < 20; i++) {
			ports[i] = new MountedPort(this, i, EnergyHandler.class, false)
			.setLocation(i < 18 ? .125 : .875, .375 + (i & 1) * .25, 0.0, EnumFacing.NORTH)
			.setName("port.rs_ctr.energy_io");
		}
	}

	@Override
	public Object getPortCallback(int pin) {
		if (node == null) return pin < 16 ? (SignalHandler)(val)-> {
			if (val == state[pin]) return;
			state[pin] = val;
		} : EnergyHandler.NOP;
		return pin < 16 ? (SignalHandler)(val) -> {
			if (val == state[pin]) return;
			state[pin] = val;
			if ((interrupt >> pin & 1) != 0)
				((Node)node).sendToAddress(target, "computer.signal", "rs_change", pin, val);
		} : this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if (callback instanceof SignalHandler)
			(out[pin] = (SignalHandler)callback).updateSignal(state[pin]);
		else out[pin] = null;
	}

	@Override
	protected void resetPin(int pin) {
		if (pin < 16)
			((SignalHandler)getPortCallback(pin)).updateSignal(0);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode == SAVE) {
			if (node != null) ComputerAPI.saveNode(node, nbt);
			nbt.setString("event", target);
			nbt.setByte("int", (byte)interrupt);
			nbt.setByte("update", (byte)update);
			nbt.setIntArray("io", state);
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			if (node != null) ComputerAPI.readNode(node, nbt);
			target = nbt.getString("event");
			interrupt = nbt.getByte("int") & 0xff;
			update = nbt.getByte("update");
			int[] arr = nbt.getIntArray("io");
			System.arraycopy(arr, 0, state, 0, Math.min(arr.length, state.length));
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (update != 0 && !world.isRemote) {
			TickRegistry.schedule(this);
		}
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		ComputerAPI.removeOCnode(node);
	}

	@Override
	public Node node() {
		return (Node)node;
	}

	@Override
	public void onConnect(Node arg0) {}

	@Override
	public void onDisconnect(Node arg0) {}

	@Override
	public void onMessage(Message arg0) {}

	@Override
	public void update() {
		if (node == null) return;
		ComputerAPI.update(this, node, 0);
	}

	@Override
	public void process() {
		synchronized(state) {
			for (int i = 0; i < 8; i++)
				if ((update >> i & 1) != 0 && out[i] != null)
					out[i].updateSignal(state[i]);
			update = 0;
		}
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 100, doc = "function(port:number):number -- returns the signal currently received at given input port [0-7].")
	public Object[] getInput(Context context, Arguments args) throws Exception {
		int i = args.checkInteger(0);
		return i >= 0 && i < 8 ? new Object[] {state[i + 8]} : null;
	}

	@SuppressWarnings("unchecked")
	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 25, doc = "function(port:number, value:number) -- sets the signal value of given output port [0-7]. Signal update happens next tick.")
	public Object[] setOutput(Context context, Arguments args) throws Exception {
		synchronized(state) {
			if (args.isTable(0))
				for (Entry<Number, Number> e : ((Map<Number, Number>)args.checkTable(0)).entrySet())
					setOut(e.getKey().intValue(), e.getValue().intValue());
			else setOut(args.checkInteger(0), args.checkInteger(1));
		}
		return null;
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, doc = "function(bitmask:number) -- registers the caller to receive {\"rs_change\", port, value} events when any of the input ports specified by bitmask changes.")
	public Object[] registerEvent(Context context, Arguments args) throws Exception {
		interrupt = args.checkInteger(0);
		target = context.node().address();
		return null;
	}

	private void setOut(int i, int v) {
		if (i < 0 || i >= 8 || state[i] == v) return;
		state[i] = v;
		if (update == 0)
			TickRegistry.schedule(this);
		update |= 1 << i;
	}

	@Override
	@Method(modid = "opencomputers")
	public int changeEnergy(int dE, boolean sim) {
		if (!(node instanceof Connector)) return 0;
		Connector c = (Connector)node;
		double d = (double)dE / OC_UNIT;
		if (!sim) return (int)Math.rint(c.changeBuffer(d) * OC_UNIT);
		d += c.globalBuffer();
		if (dE > 0) {
			d -= c.globalBufferSize();
			if (d <= 0) return dE;
			return dE - (int)Math.ceil(d * OC_UNIT);
		}
		if (d >= 0) return dE;
		return dE - (int)Math.floor(d * OC_UNIT);
	}

	@Method(modid = "opencomputers")
	public static void registerAPI() {
		Driver.add((stack) -> stack.getItem() == oc_adapter ? OC_Adapter.class : null);
	}

}
