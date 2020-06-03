package cd4017be.rs_ctr.tileentity;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import cd4017be.api.rs_ctr.com.DelayedSignal;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rs_ctr.circuit.CompiledCircuit;
import cd4017be.rs_ctr.circuit.UnloadedCircuit;
import cd4017be.rs_ctr.gui.BlockButton;
import cd4017be.rs_ctr.gui.GuiProcessor;
import cd4017be.rscpl.util.StateBuffer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class Processor extends WallMountGate implements IUpdatable, ITilePlaceHarvest, IGuiHandlerTile, IStateInteractionHandler {

	public static int BURNOUT_INTERVAL = 50;

	ItemStack[] ingreds = new ItemStack[0];
	int[] stats = new int[6];
	String name = "";
	BlockButton coreBtn = new BlockButton(null, ()-> null, ()-> name + "\n" + getError()) {
		@Override
		public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
			if (!hit)
				GuiNetworkHandler.openBlockGui(player, pos, 0);
			return true;
		}
	}.setSize(0.25F, 0.25F);
	public Circuit circuit;
	SignalHandler[] callbacks;
	EnergyHandler energySup;
	private long lastTick = 0;
	public int energy, cap, usage, gain;
	public byte tick;
	public String lastError;
	DelayedSignal delayed;
	String[] keys = new String[0];

	{ports = new MountedPort[] {new MountedPort(this, 0, EnergyHandler.class, true)};}

	@Override
	public void process() {
		tick = 0;
		if (unloaded) return;
		long t = world.getTotalWorldTime();
		if (lastTick > t) return;
		if (usage > 0) {
			int e = energy - usage + (int)(t - lastTick) * gain;
			if (e >= 0) energy = e <= cap ? e : cap;
			else if (energySup != null && (e -= energySup.changeEnergy(e - cap, false)) >= 0) energy = e;
			else {
				doBurnout(false);
				lastError = "power depleted";
				energy = BURNOUT_INTERVAL * gain;
				markDirty(SYNC);
				return;
			}
		}
		lastTick = t;
		try {
			int d = circuit.tick();
			for (; delayed != null; delayed = delayed.next, d |= 1)
				circuit.inputs[delayed.id] = delayed.value;
			if ((d & 1) != 0) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			}
			d >>>= 1;
			for (int i = 0; d != 0; i++, d >>>= 1)
				if ((d & 1) != 0 && callbacks[i] != null)
					callbacks[i].updateSignal(circuit.outputs[i]);
			if (lastError != null) {
				lastError = null;
				markDirty(SYNC);
			}
		} catch(Throwable e) {
			lastError = circuit.processError(e, this);
			if (lastError == null) {
				lastError = "BUG! see log";
				doBurnout(true);
			} else doBurnout(false);
			markDirty(SYNC);
		}
	}

	public void doBurnout(boolean hard) {
		Random rand = world.rand;
		world.playSound((EntityPlayer)null, pos, hard ? SoundEvents.ENTITY_GENERIC_EXPLODE : SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.BLOCKS, hard ? 5F : 0.5F, 2.6F + (rand.nextFloat() - rand.nextFloat()) * 0.8F);
		for (int i = 0; i < 5; ++i) {
			double d0 = (double)pos.getX() + rand.nextDouble() * 0.6D + 0.2D;
			double d1 = (double)pos.getY() + rand.nextDouble() * 0.6D + 0.2D;
			double d2 = (double)pos.getZ() + rand.nextDouble() * 0.6D + 0.2D;
			world.spawnParticle(hard ? EnumParticleTypes.SMOKE_LARGE : EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
		}
		lastTick = hard ? Long.MAX_VALUE : world.getTotalWorldTime() + BURNOUT_INTERVAL;
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		int[] inputs = circuit.inputs;
		return circuit.isInterrupt(pin) ?
			(val)-> {
				if (inputs[pin] == val) return;
				if (tick == 0) {
					tick = TickRegistry.TICK;
					TickRegistry.schedule(this);
				} else if (tick != TickRegistry.TICK) {
					delayed = new DelayedSignal(pin, val, delayed);
					return;
				}
				inputs[pin] = val;
			} : (val)-> inputs[pin] = val;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if (pin == ports.length - 1)
			energySup = callback instanceof EnergyHandler ? (EnergyHandler)callback : null;
		else {
			SignalHandler scb = callback instanceof SignalHandler ? (SignalHandler)callback : null;
			pin -= circuit.inputs.length;
			callbacks[pin] = scb;
			if (scb != null)
				scb.updateSignal(circuit.outputs[pin]);
		}
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).updateSignal(0);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode <= CLIENT || mode == ITEM) {
			nbt.merge(circuit.serializeNBT());
			NBTTagList names = new NBTTagList();
			for (MountedPort port : ports) {
				String n = port.name;
				names.appendTag(new NBTTagString(n.isEmpty() ? n : n.substring(1)));
			}
			nbt.setTag("labels", names);
			nbt.setString("name", name);
			nbt.setIntArray("stats", stats);
			if (mode != CLIENT)
				nbt.setTag("ingr", ItemFluidUtil.saveItems(ingreds));
			if (mode != ITEM)
				nbt.setInteger("energy", energy);
		} else if (mode == SYNC) {
			if (lastError != null)
				nbt.setString("err", lastError);
		}
		if (mode == SAVE) {
			nbt.setLong("burnout", lastTick);
			nbt.setBoolean("active", tick != 0);
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode <= CLIENT || mode == ITEM) {
			circuit = mode == ITEM ? new CompiledCircuit() : new UnloadedCircuit();
			circuit.deserializeNBT(nbt);
			NBTTagList names = nbt.getTagList("labels", NBT.TAG_STRING);
			int in = circuit.inputs.length, out = circuit.outputs.length;
			int oin = (4 - in) >> 1, oout = ((4 - out) >> 1) - in;
			out += in;
			ports = new MountedPort[out + 1];
			for (int i = 0; i < out; i++) {
				int j = i + (i < in ? oin : oout);
				int k = j < 0 ? 0 : j > 3 ? 3 : j;
				j = k > j ? k - j : j - k;
				ports[i] = new MountedPort(this, i, SignalHandler.class, i >= in).setLocation(i < in ? 0.125 + j * 0.25 : 0.875 - j * 0.25, 0.875 - k * 0.25, 0.25, EnumFacing.SOUTH, o).setName("\\" + names.getStringTagAt(i));
			}
			ports[out] = new MountedPort(this, out, EnergyHandler.class, true).setLocation(0.5, 1.0, 0.125, EnumFacing.UP, o).setName("port.rs_ctr.energy_i");
			name = nbt.getString("name");
			keys = circuit.getState().nbt.getKeySet().toArray(keys);
			Arrays.sort(keys);
			{int[] arr = nbt.getIntArray("stats");
			System.arraycopy(arr, 0, stats, 0, Math.min(arr.length, stats.length));}
			if (mode != CLIENT)
				ingreds = ItemFluidUtil.loadItems(nbt.getTagList("ingr", NBT.TAG_COMPOUND));
			energy = nbt.getInteger("energy");
			gain = stats[4];
			cap = stats[5];
			if ((usage = stats[0] + stats[1]) < gain)
				usage = 0;
		} else if (mode == SYNC) {
			lastError = nbt.hasKey("err", NBT.TAG_STRING) ? nbt.getString("err") : null;
		}
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			lastTick = nbt.getLong("burnout");
			tick = (byte)(nbt.getBoolean("active") ? 1 : 0);
		}
	}

	@Override
	public void onLoad() {
		if (circuit == null) circuit = new UnloadedCircuit();
		if (!world.isRemote) {
			circuit = circuit.load();
			callbacks = new SignalHandler[circuit.outputs.length];
			if (tick == 1) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			}
		}
		super.onLoad();
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(coreBtn);
	}

	@Override
	protected void orient(Orientation o) {
		coreBtn.setLocation(0.5, 0.5, 0.4375, o);
		super.orient(o);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt == null) return;
		loadState(nbt, ITEM);
		energy = cap;
		lastTick = world.getTotalWorldTime();
		tick = 1;
		onLoad();
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		return makeDefaultDrops();
	}

	@Override
	public String getName() {
		return "\\" + name;
	}

	public String getError() {
		return lastError == null ? "\u00a7ano error" : "\u00a7c" + lastError;
	}

	public String[] getIOLabels() {
		String[] io = new String[ports.length - 1];
		for (int i = 0; i < io.length; i++) {
			String n = ports[i].name;
			io[i] = n.isEmpty() ? n : n.substring(1);
		}
		return io;
	}

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		return new AdvancedContainer(this, StateSynchronizer.builder()
				.addFix(1, 4)
				.addMulFix(4, circuit.inputs.length + circuit.outputs.length)
				.addVar(keys.length)
				.build(world.isRemote), player);
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.writeInt(tick > 0 ? 1 : 0).writeInt(Math.min(cap, energy + (int)(world.getTotalWorldTime() - lastTick - 1) * gain));
		state.writeIntArray(circuit.inputs).writeIntArray(circuit.outputs).endFixed();
		NBTTagCompound nbt = circuit.getState().nbt;
		for (String key : keys) {
			Utils.writeTag(state.buffer, nbt.getTag(key));
			state.put();
		}
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		tick = (byte)state.get(tick);
		energy = state.get(energy);
		circuit.inputs = state.get(circuit.inputs);
		circuit.outputs = state.get(circuit.outputs);
		NBTTagCompound nbt = circuit.getState().nbt;
		for (String key : keys)
			if (state.next())
				nbt.setTag(key, Utils.readTag(state.buffer, nbt.getTagId(key)));
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return canPlayerAccessUI(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiProcessor getGuiScreen(EntityPlayer player, int id) {
		return new GuiProcessor(this, player);
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		int var = pkt.readUnsignedByte();
		if (var == 255) {
			if (tick == 0) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			}
			return;
		}
		String key;
		if (var >= keys.length || (key = keys[var]) == null) return;
		StateBuffer state = circuit.getState();
		state.nbt.setTag(key, Utils.readTag(pkt, state.nbt.getTagId(key)));
		circuit.setState(state);
	}

	@Override
	public Object getState(int id) {
		return id < circuit.inputs.length ? circuit.inputs[id]
			: (id -= circuit.inputs.length) < circuit.outputs.length ? circuit.outputs[id]
			: null;
	}

}
