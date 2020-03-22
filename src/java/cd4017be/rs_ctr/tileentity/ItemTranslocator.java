package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * @author CD4017BE
 *
 */
public class ItemTranslocator extends WallMountGate implements ITickableServerOnly {

	public static int BASE_COST = -100, TRANSFER_COST = -400;

	BlockReference ref0, ref1;
	EnergyHandler energy = EnergyHandler.NOP;
	SignalHandler out;
	int slot0, slot1, amIn, amOut, clk;
	boolean update;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setLocation(0.875, 0.625, 0.25, EnumFacing.NORTH).setName("port.rs_ctr.inv0"),
			new MountedPort(this, 1, BlockHandler.class, false).setLocation(0.125, 0.625, 0.25, EnumFacing.NORTH).setName("port.rs_ctr.inv1"),
			new MountedPort(this, 2, EnergyHandler.class, true).setLocation(0.375, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.energy_i"),
			new MountedPort(this, 3, SignalHandler.class, false).setLocation(0.875, 0.875, 0.25, EnumFacing.NORTH).setName("port.rs_ctr.slot0"),
			new MountedPort(this, 4, SignalHandler.class, false).setLocation(0.125, 0.875, 0.25, EnumFacing.NORTH).setName("port.rs_ctr.slot1"),
			new MountedPort(this, 5, SignalHandler.class, false).setLocation(0.625, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.clk"),
			new MountedPort(this, 6, SignalHandler.class, false).setLocation(0.875, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.am_i"),
			new MountedPort(this, 7, SignalHandler.class, true).setLocation(0.125, 0.25, 0.125, EnumFacing.UP).setName("port.rs_ctr.am_o"),
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		switch(pin) {
		case 0: return (BlockHandler)(ref)-> ref0 = ref;
		case 1: return (BlockHandler)(ref)-> ref1 = ref;
		case 3: return (SignalHandler)(val)-> slot0 = val;
		case 4: return (SignalHandler)(val)-> slot1 = val;
		case 5: return (SignalHandler)(val)-> {
			if (val == clk) return;
			clk = val;
			update = true;
		};
		case 6: return (SignalHandler)(val)-> amIn = val;
		default: return null;
		}
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		switch(pin) {
		case 2:
			energy = callback instanceof EnergyHandler ? (EnergyHandler)callback : EnergyHandler.NOP;
			break;
		case 7:
			if (callback instanceof SignalHandler) {
				out = (SignalHandler)callback;
				out.updateSignal(amOut);
			} else out = null;
			break;
		}
	}

	@Override
	protected void resetPin(int pin) {
		switch(pin) {
		case 0: ref0 = null; break;
		case 1: ref1 = null; break;
		case 3: slot0 = 0; break;
		case 4: slot1 = 0; break;
		case 5: clk = 0; break;
		case 6: amIn = 0; break;
		}
	}

	@Override
	public void update() {
		if (!update) return;
		update = false;
		int n = transfer(amIn);
		if (amOut != n) {
			amOut = n;
			if (out != null)
				out.updateSignal(n);
		}
	}

	private int transfer(int am) {
		if (am == 0 || ref0 == null || ref1 == null || !ref0.isLoaded() || !ref1.isLoaded()) return 0;
		int e = cost(am);
		if (energy.changeEnergy(e, true) != e) return 0;
		IItemHandler inv0 = ref0.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		IItemHandler inv1 = ref1.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		if (am > 0) am = transfer(am, inv0, slot0, inv1, slot1);
		else am = -transfer(-am, inv1, slot1, inv0, slot0);
		energy.changeEnergy(cost(am), false);
		return am;
	}

	private static int cost(int am) {
		if (am < 0) am = -am;
		return (int)Math.min((long)am * (long)TRANSFER_COST / 64L + (long)BASE_COST, Integer.MAX_VALUE);
	}

	private int transfer(int am, IItemHandler fromInv, int fromSlot, IItemHandler toInv, int toSlot) {
		if (fromInv == null || toInv == null || fromSlot >= fromInv.getSlots() || toSlot >= toInv.getSlots() || (fromSlot < 0 && toSlot < 0)) return 0;
		if (fromSlot < 0) {
			int n = am;
			ItemStack stack = ItemFluidUtil.drain(
				fromInv,
				(s)-> n - toInv.insertItem(toSlot, ItemHandlerHelper.copyStackWithSize(s, n), true).getCount()
			);
			if ((am = stack.getCount()) > 0)
				toInv.insertItem(toSlot, stack, false);
		} else {
			ItemStack stack = fromInv.extractItem(fromSlot, am, true);
			if (
				(am = stack.getCount()) > 0
				&& (am -= (
					toSlot >= 0 ? toInv.insertItem(toSlot, stack, false)
					: ItemHandlerHelper.insertItemStacked(toInv, stack, false)
				).getCount()) > 0
			) fromInv.extractItem(fromSlot, am, false);
		}
		return am;
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) {
			nbt.setBoolean("update", update);
			nbt.setInteger("in", amIn);
			nbt.setInteger("out", amOut);
			nbt.setInteger("clk", clk);
			nbt.setInteger("slot0", slot0);
			nbt.setInteger("slot1", slot1);
			if (ref0 != null) nbt.setTag("inv0", ref0.serializeNBT());
			else nbt.removeTag("inv0");
			if (ref1 != null) nbt.setTag("inv1", ref1.serializeNBT());
			else nbt.removeTag("inv1");
		}
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) {
			update = nbt.getBoolean("update");
			amIn = nbt.getInteger("in");
			amOut = nbt.getInteger("out");
			clk = nbt.getInteger("clk");
			slot0 = nbt.getInteger("slot0");
			slot1 = nbt.getInteger("slot1");
			ref0 = nbt.hasKey("inv0", NBT.TAG_COMPOUND) ?
				new BlockReference(nbt.getCompoundTag("inv0")) : null;
			ref1 = nbt.hasKey("inv1", NBT.TAG_COMPOUND) ?
				new BlockReference(nbt.getCompoundTag("inv1")) : null;
		}
		super.loadState(nbt, mode);
	}

	@Override
	public Object getState(int id) {
		switch(id) {
		case 0: return ref0;
		case 1: return ref1;
		case 3: return slot0;
		case 4: return slot1;
		case 5: return clk;
		case 6: return amIn;
		case 7: return amOut;
		default: return null;
		}
	}

}
