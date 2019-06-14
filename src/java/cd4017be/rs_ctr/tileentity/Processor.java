package cd4017be.rs_ctr.tileentity;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

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
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.api.DelayedSignal;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
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

	String name = "";
	BlockButton coreBtn = new BlockButton(null, ()-> null, ()-> name + "\n" + getError()) {
		@Override
		public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
			GuiNetworkHandler.openBlockGui(player, pos, 0);
			return true;
		}
	}.setSize(0.25F, 0.25F);
	public Circuit circuit;
	IntConsumer[] callbacks;
	private long burnoutTime = -1;
	public byte tick;
	public String lastError;
	DelayedSignal delayed;
	String[] keys = new String[0];

	{ports = new MountedSignalPort[0];}

	@Override
	public void process() {
		tick = 0;
		if (unloaded) return;
		if (burnoutTime > 0) {
			if (burnoutTime > world.getTotalWorldTime()) return;
			burnoutTime = -1;
		}
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
					callbacks[i].accept(circuit.outputs[i]);
			if (lastError != null) {
				lastError = null;
				markDirty(SYNC);
			}
		} catch(ArithmeticException e) {
			doBurnout(false);
			lastError = e.getMessage();
			markDirty(SYNC);
		} catch(Throwable e) {
			Main.LOG.error("Critical processor failure!", e);
			Main.LOG.error("Location: {}\nDevice details:\n{}", pos, circuit);
			doBurnout(true);
			lastError = "<critical crash>";
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
		burnoutTime = hard ? Long.MAX_VALUE : world.getTotalWorldTime() + BURNOUT_INTERVAL;
	}

	@Override
	public IntConsumer getPortCallback(int pin) {
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
	public void setPortCallback(int pin, IntConsumer callback) {
		pin -= circuit.inputs.length;
		callbacks[pin] = callback;
		if (callback != null)
			callback.accept(circuit.outputs[pin]);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode <= CLIENT || mode == ITEM) {
			nbt.merge(circuit.serializeNBT());
			NBTTagList names = new NBTTagList();
			for (MountedSignalPort port : ports)
				names.appendTag(new NBTTagString(port.name.substring(1)));
			nbt.setTag("labels", names);
			nbt.setString("name", name);
		} else if (mode == SYNC) {
			if (lastError != null)
				nbt.setString("err", lastError);
		}
		if (mode == SAVE) {
			nbt.setLong("burnout", burnoutTime);
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
			ports = new MountedSignalPort[in + out];
			for (int i = 0; i < ports.length; i++)
				ports[i] = new MountedSignalPort(this, i, i >= in).setName("\\" + names.getStringTagAt(i));
			name = nbt.getString("name");
			keys = circuit.getState().nbt.getKeySet().toArray(keys);
			Arrays.sort(keys);
		} else if (mode == SYNC) {
			lastError = nbt.hasKey("err", NBT.TAG_STRING) ? nbt.getString("err") : null;
		}
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			burnoutTime = nbt.getLong("burnout");
			tick = (byte)(nbt.getBoolean("active") ? 1 : 0);
		}
	}

	@Override
	protected void setupData() {
		if (circuit == null) circuit = new UnloadedCircuit();
		if (!world.isRemote) {
			circuit = circuit.load();
			callbacks = new IntConsumer[circuit.outputs.length];
			if (tick == 1) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			}
		}
		super.setupData();
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(coreBtn);
	}

	@Override
	protected void orient() {
		coreBtn.setLocation(0.5, 0.5, 0.4375, o);
		int in = circuit.inputs.length, out = circuit.outputs.length;
		int oin = (4 - in) >> 1, oout = ((4 - out) >> 1) - in;
		for (int i = 0; i < ports.length; i++) {
			int j = i + (i < in ? oin : oout);
			int k = j < 0 ? 0 : j > 3 ? 3 : j;
			j = k > j ? k - j : j - k;
			ports[i].setLocation(i < in ? 0.125 + j * 0.25 : 0.875 - j * 0.25, 0.875 - k * 0.25, 0.25, EnumFacing.SOUTH, o);
		}
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt == null) return;
		clearData();
		loadState(nbt, ITEM);
		tick = 1;
		setupData();
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
		String[] io = new String[ports.length];
		for (int i = 0; i < io.length; i++)
			io[i] = ports[i].name.substring(1);
		return io;
	}

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		return new AdvancedContainer(this, StateSynchronizer.builder()
				.addFix(1)
				.addMulFix(4, circuit.inputs.length + circuit.outputs.length)
				.addVar(keys.length)
				.build(world.isRemote), player);
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.writeInt(tick > 0 ? 1 : 0);
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
		circuit.inputs = state.get(circuit.inputs);
		circuit.outputs = state.get(circuit.outputs);
		NBTTagCompound nbt = circuit.getState().nbt;
		for (String key : keys)
			if (state.next())
				nbt.setTag(key, Utils.readTag(state.buffer, nbt.getTagId(key)));
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return !unloaded && !player.isDead && player.getDistanceSqToCenter(pos) < 256.0;
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

}
