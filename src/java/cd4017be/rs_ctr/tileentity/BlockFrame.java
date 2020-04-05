package cd4017be.rs_ctr.tileentity;

import java.util.ArrayList;
import cd4017be.api.rs_ctr.frame.IFrame;
import cd4017be.api.rs_ctr.frame.IFrameOperator;
import cd4017be.lib.tileentity.BaseTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
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
		markDirty(SAVE);
	}

	@Override
	public void unlink(BlockPos pos) {
		linked.remove(pos);
		markDirty(SAVE);
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
		if (mode == SAVE) {
			NBTTagList list = new NBTTagList();
			for (BlockPos l : linked) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("x", l.getX() - pos.getX());
				tag.setInteger("y", l.getY() - pos.getY());
				tag.setInteger("z", l.getZ() - pos.getZ());
				list.appendTag(tag);
			}
			nbt.setTag("links", list);
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			linked.clear();
			if (nbt.hasKey("link", NBT.TAG_LIST)) {//backwards compatibility
				NBTTagList list = nbt.getTagList("link", NBT.TAG_COMPOUND);
				for (int i = 0; i < list.tagCount(); i++) {
					NBTTagCompound tag = list.getCompoundTagAt(i);
					linked.add(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
				}
			} else {
				NBTTagList list = nbt.getTagList("links", NBT.TAG_COMPOUND);
				for (int i = 0; i < list.tagCount(); i++) {
					NBTTagCompound tag = list.getCompoundTagAt(i);
					linked.add(pos.add(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
				}
			}
		}
	}

}
