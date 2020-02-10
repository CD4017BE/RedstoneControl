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
	public static int MAX_BUFFER = 64;

	private Object node = ComputerAPI.newOCnode(this, "rsio", true);
	private final SignalHandler[] out = new SignalHandler[8];
	private final int[] state = new int[16];
	private int update, interrupt;
	private String target = "";
	private int[] bufIN, bufOUT;
	private long changes;
	private int nIN, nOUT, tIN, tOUT, time;
	private byte tick;

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
			int ipin = pin - 8;
			if ((interrupt >> ipin & 1) != 0)
				((Node)node).sendToAddress(target, "computer.signal", "rs_change", ipin, val);
			//late buffer signal update
			if (ipin < nIN && tick == TickRegistry.TICK)
				bufIN[ipin + (time & tIN) * nIN] = val;
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
			nbt.setInteger("time", time);
			if (bufIN != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("n", (byte)nIN);
				tag.setByte("t", (byte)tIN);
				tag.setIntArray("buf", bufIN);
				nbt.setTag("in", tag);
			}
			if (bufOUT != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("n", (byte)nOUT);
				tag.setByte("t", (byte)tOUT);
				tag.setIntArray("buf", bufOUT);
				tag.setLong("c", changes);
				nbt.setTag("out", tag);
			}
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
			NBTTagCompound tag = nbt.getCompoundTag("in");
			nIN = tag.getByte("n");
			tIN = tag.getByte("t");
			bufIN = tag.getIntArray("buf");
			if (nIN == 0 || arr.length != nIN * (tIN + 1)) {
				bufIN = null;
				nIN = tIN = 0;
			}
			tag = nbt.getCompoundTag("out");
			nOUT = tag.getByte("n");
			tOUT = tag.getByte("t");
			bufOUT = tag.getIntArray("buf");
			changes = tag.getLong("c");
			if (nOUT == 0 || arr.length != nOUT * (tOUT + 1)) {
				bufOUT = null;
				nOUT = tOUT = 0;
			}
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
		time++;
		tick = TickRegistry.TICK;
		if (bufIN != null) {
			int t = (time & tIN) * nIN;
			for (int i = nIN - 1; i >= 0; i--)
				bufIN[i + t] = state[i + 8];
		}
		if (bufOUT != null) {
			int t = (time & tOUT) * nOUT;
			int c = (int)(changes >> t) & 0xff >> 8 - nOUT;
			if (c != 0) {
				changes ^= (long)c << t;
				for (int i = nOUT - 1; i >= 0; i--)
					if ((c >> i & 1) != 0)
						setOut(i, bufOUT[i + t]);
			}
		}
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
	@Callback(direct = true, limit = 100, doc = "function(port:number[, time:number]):number -- returns the signal currently (or at the given buffered time < time()) received at given input port [0-7].")
	public Object[] getInput(Context context, Arguments args) throws Exception {
		int i = args.checkInteger(0);
		if (i < 0 || i >= 8) return null;
		if (args.count() > 1 && i < nIN) {
			int t = args.checkInteger(1);
			return new Object[] {
				time() > t && time - t <= tIN
					? bufIN[i + (t & tIN) * nIN]
					: Double.NaN
			};
		}
		return new Object[] {state[i + 8]};
	}

	@SuppressWarnings("unchecked")
	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 25, doc = "function(port:number[, time:number], value:number):bool -- sets the signal value of given output port [0-7] for the next tick (or given future time > time()).")
	public Object[] setOutput(Context context, Arguments args) throws Exception {
		if (args.count() > 2) {
			int i = args.checkInteger(0);
			if (i < 0 || i >= nOUT) return null;
			int t = args.checkInteger(1) - 1, v = args.checkInteger(2);
			if (t - time > tOUT + 1)
				return new Object[] {false};
			else if (t > time) {
				i += (t & tOUT) * nOUT;
				bufOUT[i] = v;
				changes |= 1L << i;
			} else synchronized(state) {
				setOut(i, v);
			}
		} else synchronized(state) {
			if (args.isTable(0))
				for (Entry<Number, Number> e : ((Map<Number, Number>)args.checkTable(0)).entrySet())
					setOut(e.getKey().intValue(), e.getValue().intValue());
			else setOut(args.checkInteger(0), args.checkInteger(1));
		}
		return new Object[] {true};
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, doc = "function(bitmask:number) -- registers the caller to receive {\"rs_change\", port, value} events when any of the input ports specified by bitmask changes.")
	public Object[] registerEvent(Context context, Arguments args) throws Exception {
		interrupt = args.checkInteger(0);
		target = context.node().address();
		return null;
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, doc = "function([setTime:number]):number -- returns the internal timer in ticks or resets it to the given value")
	public Object[] time(Context context, Arguments args) throws Exception {
		if (args.count() > 0)
			time = args.checkInteger(0);
		return new Object[] {time()};
	}

	private int time() {
		return tick == TickRegistry.TICK ? time : time + 1;
	}

	@Method(modid = "opencomputers")
	@Callback(direct = false, doc = "function(inputs:number, ticks:number) -- configures the first given number of inputs to be buffered over given number of ticks to support tick precise reading.")
	public Object[] bufferInput(Context context, Arguments args) throws Exception {
		int n = args.checkInteger(0);
		if (n <= 0) {
			nIN = 0;
			tIN = 0;
			bufIN = null;
		} else {
			if (n > 8) n = 8;
			int t = (Integer.highestOneBit(args.checkInteger(1) - 1) << 1);
			if (t <= 1 || t * n > MAX_BUFFER) throw new Exception("buffer size out of range 2*inputs..." + MAX_BUFFER);
			nIN = n;
			tIN = t - 1;
			bufIN = new int[n * t];
		}
		return null;
	}

	@Method(modid = "opencomputers")
	@Callback(direct = false, doc = "function(inputs:number, ticks:number) -- configures the first given number of outputs to be buffered over given number of ticks to support tick precise writing.")
	public Object[] bufferOutput(Context context, Arguments args) throws Exception {
		int n = args.checkInteger(0);
		if (n <= 0) {
			nOUT = 0;
			tOUT = 0;
			bufOUT = null;
		} else {
			if (n > 8) n = 8;
			int t = (Integer.highestOneBit(args.checkInteger(1) - 1) << 1);
			if (t <= 1 || t * n > MAX_BUFFER) throw new Exception("buffer size out of range 2*inputs..." + MAX_BUFFER);
			nOUT = n;
			tOUT = t - 1;
			bufOUT = new int[n * t];
			changes = 0;
		}
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
