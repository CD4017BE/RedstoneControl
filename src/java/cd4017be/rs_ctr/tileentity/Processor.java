package cd4017be.rs_ctr.tileentity;

import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rs_ctr.circuit.CompiledCircuit;
import cd4017be.rs_ctr.circuit.UnloadedCircuit;
import cd4017be.rs_ctr.gui.BlockButton;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.util.Constants.NBT;


/**
 * @author CD4017BE
 *
 */
public class Processor extends WallMountGate implements IUpdatable, ITilePlaceHarvest {

	public static int BURNOUT_INTERVAL = 50;

	String name = "";
	BlockButton coreBtn = new BlockButton(null, ()-> null, ()-> name);
	Circuit circuit;
	IntConsumer[] callbacks;
	private long burnoutTime = -1;
	boolean update;

	{ports = new MountedSignalPort[0];}

	@Override
	public void process() {
		update = false;
		if (unloaded) return;
		if (burnoutTime > 0) {
			if (burnoutTime > world.getTotalWorldTime()) return;
			burnoutTime = -1;
		}
		try {
			int d = circuit.tick();
			if ((d & 1) != 0) {
				TickRegistry.schedule(this);
				update = true;
			}
			d >>>= 1;
			for (int i = 0; d != 0; i++, d >>>= 1)
				if ((d & 1) != 0 && callbacks[i] != null)
					callbacks[i].accept(circuit.outputs[i]);
		} catch(ArithmeticException e) {
			doBurnout(false);
		} catch(Throwable e) {
			Main.LOG.error("Critical processor failure!", e);
			Main.LOG.error("Location: {}\nDevice details:\n{}", pos, circuit);
			doBurnout(true);
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
				inputs[pin] = val;
				if (update) return;
				TickRegistry.schedule(this);
				update = true;
			} : (val)-> inputs[pin] = val;
	}

	@Override
	public void setPortCallback(int pin, IntConsumer callback) {
		callbacks[pin - circuit.inputs.length] = callback;
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
		}
		if (mode == SAVE) {
			nbt.setLong("burnout", burnoutTime);
			nbt.setBoolean("active", update);
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
		}
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			burnoutTime = nbt.getLong("burnout");
			update = nbt.getBoolean("active");
		}
	}

	@Override
	protected void setupData() {
		if (circuit == null) circuit = new UnloadedCircuit();
		if (!world.isRemote) {
			circuit = circuit.load();
			callbacks = new IntConsumer[circuit.outputs.length];
			if (update && unloaded) TickRegistry.schedule(this);
		}
		super.setupData();
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(coreBtn);
	}

	protected void orient() {
		coreBtn.setLocation(0.5, 0.5, 0.4375, o);
		int in = circuit.inputs.length, out = circuit.outputs.length;
		int oin = (4 - in) >> 1, oout = (5 - out) >> 1 - in;
		for (int i = 0; i < ports.length; i++) {
			int j = i + (i < in ? oin : oout);
			int k = j < 0 ? 0 : j > 3 ? 3 : j;
			j = k > j ? k - j : j - k;
			ports[i].setLocation(i < in ? 0.125 + j * 0.25 : 0.875 - j * 0.25, 0.125 + k * 0.25, 0.25, EnumFacing.SOUTH, o);
		}
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt == null) return;
		clearData();
		loadState(nbt, ITEM);
		if (!update) unloaded = true;
		setupData();
		unloaded = false;
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		return makeDefaultDrops();
	}

}
