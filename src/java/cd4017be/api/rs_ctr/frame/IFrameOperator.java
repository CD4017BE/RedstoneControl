package cd4017be.api.rs_ctr.frame;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

/** @author cd4017be */
public interface IFrameOperator {

	static final EnumFacing[] XYZ_DIRS = {EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.UP, EnumFacing.SOUTH};

	void onFrameBreak(BlockPos pos);

	/**@param world
	 * @param origin scan starting position
	 * @param areaOut gets set to the area as [x0, y0, z0, dx, dy, dz]
	 * @param range maximum distance to scan
	 * @param front optional direction to exclude */
	public static void scanArea(World world, BlockPos origin, int[] areaOut, int range, EnumFacing front) {
		MutableBlockPos p = new MutableBlockPos();
		//axis scan for range markers
		int l, found = 0;
		for (int j = 0; j < 6; j++) {
			EnumFacing s = XYZ_DIRS[j];
			if (s == front || (l = scan(world, p.setPos(origin), s, range)) > range)
				areaOut[j] = 0;
			else {
				areaOut[j] = l;
				found |= 1 << j;
			}
		}
		//extended edge scan
		l = 0;
		switch(found) {
		case 0b100100: l++;
		case 0b010010: l++;
		case 0b001001:
			EnumFacing d = XYZ_DIRS[l];
			int n = areaOut[l];
			int m = -areaOut[l + 3];
			for (int j = 0; j < 6; j++) {
				if ((found >> j & 1) != 0) continue;
				EnumFacing s = XYZ_DIRS[j];
				if (s == front) continue;
				l = Math.min(
					scan(world, p.setPos(origin).move(d, n), s, range),
					scan(world, p.setPos(origin).move(d, m), s, range)
				);
				if (l <= range) areaOut[j] = l;
			}
		}
		//transform coords
		areaOut[3] += areaOut[0] - 1;
		areaOut[4] += areaOut[1] + 1;
		areaOut[5] += areaOut[2] - 1;
		areaOut[0] = origin.getX() - areaOut[0] + 1;
		areaOut[1] = origin.getY() - areaOut[1];
		areaOut[2] = origin.getZ() - areaOut[2] + 1;
	}

	static int scan(World world, MutableBlockPos p, EnumFacing s, int range) {
		int dx = s.getFrontOffsetX(), dy = s.getFrontOffsetY(), dz = s.getFrontOffsetZ();
		int x = p.getX(), y = p.getY(), z = p.getZ();
		for (int i = 1; i <= range; i++)
			if (world.getTileEntity(p.setPos(x += dx, y += dy, z += dz)) instanceof IFrame)
				return i;
		return range + 1;
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
