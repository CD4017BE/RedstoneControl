package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;

/** @author CD4017BE */
public class BlockDelayer extends WallMountGate {

	private final Delay[] channels = new Delay[4];

	{
		ports = new MountedPort[8];
		for(int i = 0; i < 4; i++) {
			ports[i] = new MountedPort(this, i, BlockHandler.class, false)
			.setLocation(0.25F, 0.125F + i * 0.25F, 0.125F, EnumFacing.WEST)
			.setName("port.rs_ctr.bi");
			ports[i + 4] = new MountedPort(this, i + 4, BlockHandler.class, true)
			.setLocation(0.75F, 0.125F + i * 0.25F, 0.125F, EnumFacing.EAST)
			.setName("port.rs_ctr.bo");
			channels[i] = new Delay();
		}
	}

	@Override
	public BlockHandler getPortCallback(int pin) {
		return channels[pin];
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		Delay c = channels[pin - 4];
		if(callback instanceof BlockHandler)
			(c.out = (BlockHandler)callback).updateBlock(c.state);
		else c.out = null;
	}

	@Override
	protected void resetPin(int pin) {
		channels[pin].updateBlock(null);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if(mode == SAVE) {
			NBTTagList list = new NBTTagList();
			for(Delay d : channels)
				list.appendTag(
					d.state != null ? d.state.serializeNBT()
						: new NBTTagCompound()
				);
			nbt.setTag("states", list);
		}
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if(mode == SAVE) {
			NBTTagList list = nbt.getTagList("states", NBT.TAG_COMPOUND);
			for(int i = 0; i < 4; i++) {
				NBTTagCompound tag = list.getCompoundTagAt(i);
				channels[i].state = tag.hasNoTags() ? null
					: new BlockReference(tag);
			}
		}
	}


	static class Delay implements BlockHandler, IUpdatable {

		BlockHandler out;
		BlockReference state;
		byte tick;

		@Override
		public void updateBlock(BlockReference value) {
			if(BlockReference.equalDelayed(value, state, 1)) return;
			if(tick == 0) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			} else if(tick != TickRegistry.TICK && out != null) {
				tick = TickRegistry.TICK;
				out.updateBlock(state);
			}
			state = BlockReference.delayed(value, 1);
		}

		@Override
		public void process() {
			if(tick == TickRegistry.TICK)
				TickRegistry.schedule(this);
			else {
				tick = 0;
				if(out != null) out.updateBlock(state);
			}
		}

	}

	@Override
	public Object getState(int id) {
		return channels[id & 3].state;
	}

}
