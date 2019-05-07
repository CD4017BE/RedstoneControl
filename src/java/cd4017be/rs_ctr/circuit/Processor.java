package cd4017be.rs_ctr.circuit;

import java.util.Random;
import java.util.function.IntConsumer;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.rs_ctr.Main;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * @author CD4017BE
 *
 */
@Deprecated
public class Processor implements INBTSerializable<NBTTagCompound>, IUpdatable {

	public static int MAX_SELF_INTERRUPTS = 4, BURNOUT_INTERVAL = 50;

	TileEntity host;
	protected boolean update;
	protected int selfInterrupts;
	private long burnoutTime;
	Circuit circuit;


	@Override
	public void process() {
		update = false;
		if (host == null || selfInterrupts <= 0 && host.getWorld().getTotalWorldTime() < burnoutTime) return;
		try {
			if (circuit.tick() && !update) {
				TickRegistry.schedule(this);
				update = true;
			}
			if (!update) selfInterrupts = MAX_SELF_INTERRUPTS;
			else if (--selfInterrupts <= 0) doBurnout(false);
		} catch(Exception e) {
			Main.LOG.error("Critical processor failure!", e);
			Main.LOG.error("Location: {}\nDevice details:\n{}", host.getPos(), circuit);
			doBurnout(true);
		}
	}

	public void doBurnout(boolean hard) {
		World world = host.getWorld();
		BlockPos pos = host.getPos();
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

	public IntConsumer getInputCallback(int pin) {
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
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = circuit.serializeNBT();
		if (selfInterrupts > 0) nbt.setByte("sint", (byte)selfInterrupts);
		else nbt.setLong("burnout", burnoutTime);
		nbt.setBoolean("active", update);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		circuit.deserializeNBT(nbt);
		selfInterrupts = nbt.getByte("sint") & 0xff;
		burnoutTime = nbt.getLong("burnout");
		update = nbt.getBoolean("active");
	}


}
