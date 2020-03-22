package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.IStateInfo;
import cd4017be.api.rs_ctr.port.Port;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** @author CD4017BE */
public class ItemSignalProbe extends ItemWrench {

	/** @param id */
	public ItemSignalProbe(String id) {
		super(id);
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean sel) {
		if(!sel || world.isRemote) return;
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		Vec3d p = entity.getPositionEyes(0);
		RayTraceResult rtr = world.rayTraceBlocks(p, p.add(entity.getLook(0).scale(5)));
		scanPorts: {
			if(rtr == null) {
				nbt.setInteger("tgt", 0);
				break scanPorts;
			}
			BlockPos pos = rtr.getBlockPos();
			nbt.setInteger("tgt", pos.hashCode());
			TileEntity te = world.getTileEntity(pos);
			if(!(te instanceof IStateInfo)) break scanPorts;
			IStateInfo si = (IStateInfo)te;
			NBTTagList info = new NBTTagList();
			for(Port port : si.availablePorts()) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("id", port.pin);
				Object val = null;
				try {
					val = si.getState(port.pin);
				} catch (Exception e) {}
				if(port.type == SignalHandler.class) {
					tag.setByte("type", (byte)0);
					if(val instanceof Integer)
						tag.setInteger("val", (int)val);
				} else if(port.type == BlockHandler.class) {
					tag.setByte("type", (byte)1);
					if(val instanceof BlockReference)
						tag.setTag("val", ((BlockReference)val).serializeNBT());
					else tag.setTag("val", new NBTTagCompound());
				} else continue;
				info.appendTag(tag);
			}
			nbt.setTag("ports", info);
			return;
		}
		nbt.removeTag("ports");
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return oldStack.getItem() != newStack.getItem();
	}

}
