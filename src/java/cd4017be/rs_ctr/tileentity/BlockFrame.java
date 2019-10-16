package cd4017be.rs_ctr.tileentity;

import java.util.ArrayList;
import cd4017be.api.rs_ctr.frame.IFrame;
import cd4017be.api.rs_ctr.frame.IFrameOperator;
import cd4017be.lib.tileentity.BaseTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants.NBT;

/** @author cd4017be */
public class BlockFrame extends BaseTileEntity implements IFrame {

	public ArrayList<BlockPos> linked = new ArrayList<>();

	@Override
	public ArrayList<BlockPos> getLinks() {
		return linked;
	}

	@Override
	public void link(BlockPos pos) {
		if (linked.contains(pos)) return;
		linked.add(pos.toImmutable());
		markDirty(SYNC);
	}

	@Override
	public void unlink(BlockPos pos) {
		linked.remove(pos);
		markDirty(SYNC);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (world.isRemote) return;
		for (BlockPos pos : linked) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IFrameOperator)
				((IFrameOperator)te).onFrameBreak(this.pos);
		}
		linked.clear();
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		NBTTagList list = new NBTTagList();
		for (BlockPos pos : linked) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("x", pos.getX());
			tag.setInteger("y", pos.getY());
			tag.setInteger("z", pos.getZ());
			list.appendTag(tag);
		}
		nbt.setTag("link", list);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		NBTTagList list = nbt.getTagList("link", NBT.TAG_COMPOUND);
		linked.clear();
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			linked.add(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
		}
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

}
