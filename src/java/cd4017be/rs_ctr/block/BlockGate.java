package cd4017be.rs_ctr.block;

import cd4017be.lib.block.MultipartBlock;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;


/**
 * @author CD4017BE
 *
 */
public class BlockGate extends OrientedBlock {

	/**
	 * @param id
	 * @param m
	 * @param sound
	 * @param flags
	 * @param tile
	 */
	public BlockGate(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile) {
		super(id, m, sound, flags, tile, null);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		orientProp = PropertyGateOrient.GATE_ORIENT;
		return new ExtendedBlockState(this, new IProperty[] {orientProp}, new IUnlistedProperty[] {MultipartBlock.moduleRef});
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IModularTile)
			return ((IExtendedBlockState)state).withProperty(MultipartBlock.moduleRef, ((IModularTile)te));
		else return state;
	}

}
