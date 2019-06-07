package cd4017be.rs_ctr.tileentity;

import java.util.Random;
import java.util.function.IntConsumer;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rs_ctr.circuit.UnloadedCircuit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;


/**
 * @author CD4017BE
 *
 */
public class Processor extends Gate implements IUpdatable {

	public static int BURNOUT_INTERVAL = 50;

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
		nbt.setTag("circuit", circuit.serializeNBT());
		nbt.setLong("burnout", burnoutTime);
		nbt.setBoolean("active", update);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		circuit = new UnloadedCircuit();
		circuit.deserializeNBT(nbt.getCompoundTag("circuit"));
		burnoutTime = nbt.getLong("burnout");
		update = nbt.getBoolean("active");
		super.loadState(nbt, mode);
	}

	@Override
	protected void setupData() {
		super.setupData();
		if (world.isRemote) return;
		if (circuit != null)
			circuit = circuit.load();
		else circuit = new UnloadedCircuit();
		if (update && unloaded) TickRegistry.schedule(this);
	}

}
