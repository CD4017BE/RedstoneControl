package cd4017be.rs_ctr.tileentity;

import cd4017be.lib.util.Orientation;
import net.minecraft.nbt.NBTTagCompound;


/**
 * @author CD4017BE
 *
 */
public abstract class WallMountGate extends Gate {

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode <= CLIENT) nbt.setByte("o", (byte)o.ordinal());
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode <= CLIENT) {
			o = Orientation.values()[nbt.getByte("o") & 0xf];
			orient();
		} else if (mode == ITEM) orient();
		super.loadState(nbt, mode);
	}

	@Override
	public void updateContainingBlockInfo() {
		super.updateContainingBlockInfo();
		orient();
	}

	protected abstract void orient();

}
