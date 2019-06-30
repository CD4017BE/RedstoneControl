package cd4017be.rs_ctr.api.com;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Communication object that allows devices to remotely interact with dynamically selected blocks in the world.
 * @author CD4017BE
 */
public class BlockReference {

	public final World world;
	public final BlockPos pos;
	public final EnumFacing face;

	public BlockReference(World world, BlockPos pos, EnumFacing face) {
		this.world = world;
		this.pos = pos;
		this.face = face;
	}

	/**
	 * @return whether actions on this block can be performed without potentially force loading chunks.
	 */
	public boolean isLoaded() {
		return world.isBlockLoaded(pos);
	}

	/**
	 * @param <C>
	 * @param cap the capability to obtain
	 * @return an instance of the given capability or null if not available
	 */
	public @Nullable <C> C getCapability(Capability<C> cap) {
		TileEntity te = world.getTileEntity(pos);
		if (te == null) return null;
		return te.getCapability(cap, face);
	}

	/**
	 * The callback interface for transmitting BlockReferences.
	 */
	@FunctionalInterface
	public interface BlockHandler {

		/**
		 * called when the BlockReference changes
		 * @param ref the new BlockReference
		 */
		void updateBlock(BlockReference ref);

	}

}
