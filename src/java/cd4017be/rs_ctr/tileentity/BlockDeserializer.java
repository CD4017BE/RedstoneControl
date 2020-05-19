package cd4017be.rs_ctr.tileentity;

import java.util.Arrays;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;

/** @author CD4017BE */
public class BlockDeserializer extends WallMountGate implements BlockHandler {

	private final SignalHandler[] out;
	private int refX, refY, refZ;
	private BlockReference block;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false)
			.setName("port.rs_ctr.bi").setLocation(.25, .125, .125, EnumFacing.WEST),
			new MountedPort(this, 1, SignalHandler.class, false)
			.setName("port.rs_ctr.refx").setLocation(.25, .375, .125, EnumFacing.WEST),
			new MountedPort(this, 2, SignalHandler.class, false)
			.setName("port.rs_ctr.refy").setLocation(.25, .625, .125, EnumFacing.WEST),
			new MountedPort(this, 3, SignalHandler.class, false)
			.setName("port.rs_ctr.refz").setLocation(.25, .875, .125, EnumFacing.WEST),
			new MountedPort(this, 4, SignalHandler.class, true)
			.setName("port.rs_ctr.dim").setLocation(.75, .125, .125, EnumFacing.EAST),
			new MountedPort(this, 5, SignalHandler.class, true)
			.setName("port.rs_ctr.xo").setLocation(.75, .375, .125, EnumFacing.EAST),
			new MountedPort(this, 6, SignalHandler.class, true)
			.setName("port.rs_ctr.yo").setLocation(.75, .625, .125, EnumFacing.EAST),
			new MountedPort(this, 7, SignalHandler.class, true)
			.setName("port.rs_ctr.zo").setLocation(.75, .875, .125, EnumFacing.EAST)
		};
		Arrays.fill(out = new SignalHandler[4], SignalHandler.NOP);
	}

	@Override
	public Object getPortCallback(int pin) {
		switch(pin) {
		case 1:
			return (SignalHandler)(v) -> refX = v;
		case 2:
			return (SignalHandler)(v) -> refY = v;
		case 3:
			return (SignalHandler)(v) -> refZ = v;
		default:
			return this;
		}
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		SignalHandler h = callback instanceof SignalHandler ? (SignalHandler)callback : SignalHandler.NOP;
		out[pin - 4] = h;
		h.updateSignal(out(pin - 4));
	}

	@Override
	protected void resetPin(int pin) {
		if(pin == 0)
			updateBlock(new BlockReference(world, pos, o.front));
		else((SignalHandler)getPortCallback(pin)).updateSignal(0);
	}

	@Override
	public void updateBlock(BlockReference ref) {
		if(BlockReference.equal(ref, block)) return;
		block = ref;
		for(int i = 0; i < 4; i++)
			out[i].updateSignal(out(i));
	}

	private int out(int i) {
		switch(i) {
		default:
			return block == null ? Integer.MIN_VALUE : block.dim;
		case 1:
			return block == null ? 0 : block.pos.getX() - refX;
		case 2:
			return block == null ? 0 : block.pos.getY() - refY;
		case 3:
			return block == null ? 0 : block.pos.getZ() - refZ;
		}
	}

	@Override
	public Object getState(int pin) {
		switch(pin) {
		case 0:
			return block;
		case 1:
			return refX;
		case 2:
			return refY;
		case 3:
			return refZ;
		default:
			return out(pin - 4);
		}
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode == SAVE) {
			if(block != null) nbt.setTag("block", block.serializeNBT());
			nbt.setInteger("refX", refX);
			nbt.setInteger("refY", refY);
			nbt.setInteger("refZ", refZ);
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			block = nbt.hasKey("block", NBT.TAG_COMPOUND) ? new BlockReference(nbt.getCompoundTag("block")) : null;
			refX = nbt.getInteger("refX");
			refY = nbt.getInteger("refY");
			refZ = nbt.getInteger("refZ");
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (((MountedPort)ports[0]).getConnector() == null && !world.isRemote)
			resetPin(0);
	}

}
