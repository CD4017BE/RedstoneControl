package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;

/** @author CD4017BE */
public class SolarCell extends WallMountGate implements ITickableServerOnly {

	public static int POWER = 100;

	EnergyHandler energySink;
	private byte tick;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, EnergyHandler.class, true)
			.setName("port.rs_ctr.energy_o").setLocation(0.5, 0.125, 0, EnumFacing.NORTH)
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		return null;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		energySink = callback instanceof EnergyHandler ? (EnergyHandler)callback : null;
	}

	@Override
	protected void resetPin(int pin) {}

	@Override
	public void update() {
		if(((int)world.getTotalWorldTime() & 7) != tick || energySink == null)
			return;
		Chunk c = getChunk();
		int l = c.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted();
		if(l < 0) l = 0;
		else l = (l + 2) * l + 1;
		l += c.getLightFor(EnumSkyBlock.BLOCK, pos) << 1;
		energySink.changeEnergy(l * POWER >> 5, false);
	}

	@Override
	public void setPos(BlockPos posIn) {
		super.setPos(posIn);
		tick = (byte)(posIn.getX() + posIn.getY() + posIn.getZ() & 7);
	}

	@Override
	public Object getState(int id) {
		return null;
	}

}
