package cd4017be.api.rs_ctr.frame;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author cd4017be */
public interface IFrameOperator {

	void onFrameBreak(BlockPos pos);

	@SideOnly(Side.CLIENT)
	void render(double x, double y, double z, BufferBuilder buffer);

	/**@param world
	 * @param origin scan starting position
	 * @param areaOut gets set to the area as [x0, y0, z0, dx, dy, dz]
	 * @param range maximum distance to scan
	 * @param front optional direction to exclude */
	public static void scanArea(World world, BlockPos origin, int[] areaOut, int range, EnumFacing front) {
		int x = origin.getX(), y = origin.getY(), z = origin.getZ();
		MutableBlockPos p = new MutableBlockPos();
		//scan for range markers
		for (int j = 0; j < 6; j++) {
			areaOut[j] = 0;
			EnumFacing s = EnumFacing.VALUES[((j + 2) % 3) << 1 | j / 3];
			if (s == front) continue;
			int dx = s.getFrontOffsetX(), dy = s.getFrontOffsetY(), dz = s.getFrontOffsetZ();
			int x1 = x, y1 = y, z1 = z;
			for (int i = 1; i <= range; i++) {
				if (world.getTileEntity(p.setPos(x1 += dx, y1 += dy, z1 += dz)) instanceof IFrame) {
					areaOut[j] = i;
					break;
				}
			}
		}
		//transform coords
		areaOut[3] += areaOut[0] - 1;
		areaOut[4] += areaOut[1] + 1;
		areaOut[5] += areaOut[2] - 1;
		areaOut[0] = x - areaOut[0] + 1;
		areaOut[1] = y - areaOut[1];
		areaOut[2] = z - areaOut[2] + 1;
	}

	/**@param area
	 * @param i corner index [0...7]
	 * @return position of given corner */
	public static BlockPos getCorner(int[] area, int i) {
		return new BlockPos(
			area[0] + ((i & 1) != 0 ? area[3] : -1),
			area[1] + ((i & 2) != 0 ? area[4] - 1 : 0),
			area[2] + ((i & 4) != 0 ? area[5] : -1)
		);
	}

	/**@param world
	 * @param origin
	 * @param area
	 * @return bitmap of missing corners */
	public static int checkCorners(World world, BlockPos origin, int[] area) {
		int status = 0;
		for (int i = 0; i < 8; i++) {
			BlockPos p = getCorner(area, i);
			if (p.equals(origin)) continue;
			TileEntity te = world.getTileEntity(p);
			if (te instanceof IFrame) {
				((IFrame)te).link(origin);
			} else status |= 1 << i;
		}
		return status;
	}

	/**@param world
	 * @param origin
	 * @param area
	 * @param corners */
	public static void unlinkCorners(World world, BlockPos origin, int[] area, int corners) {
		if (world.isRemote) return;
		for (int i = 0; i < 8; i++) {
			if ((corners >> i & 1) == 0) continue;
			BlockPos pos = getCorner(area, i);
			if (pos.equals(origin)) continue;
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IFrame)
				((IFrame)te).unlink(origin);
		}
	}

}
