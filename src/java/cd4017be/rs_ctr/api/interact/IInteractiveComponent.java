package cd4017be.rs_ctr.api.interact;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public interface IInteractiveComponent {

	/**
	 * @param start ray start (relative to owner location)
	 * @param dir ray direction and length
	 * @return hit direction, distance (from start) and face (or null if not hit).
	 */
	Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir);

	/**
	 * @param player the interacting player
	 * @param hit whether it was a hit (left click) instead of interaction (right click)
	 * @param side aimed component side
	 * @param aim aimed point
	 * @return consume event
	 */
	boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim);

	@SideOnly(Side.CLIENT)
	void draw(World world, BlockPos pos, double x, double y, double z, BufferBuilder buffer);

	/**
	 * @param aim aimed point
	 * @return the text to display at specified position when a player aims at the component
	 */
	Pair<Vec3d, String> getDisplayText(Vec3d aim);

	public static Pair<Vec3d, EnumFacing> rayTraceFlat(Vec3d start, Vec3d dir, Vec3d pos, EnumFacing side, float width, float height) {
		double dx = pos.x - start.x,
				dy = pos.y - start.y,
				dz = pos.z - start.z;
		double d;
		switch(side) {
		case DOWN: case UP:
			d = dy / dir.y;
			if (d <= 0 || d > 1.0 || Math.abs(dx - dir.x * d) > width || Math.abs(dz - dir.z * d) > height)
				return null;
			side = dy > 0 ? EnumFacing.DOWN : EnumFacing.UP;
			break;
		case NORTH: case SOUTH:
			d = dz / dir.z;
			if (d < 0 || d > 1.0 || Math.abs(dx - dir.x * d) > width || Math.abs(dy - dir.y * d) > height)
				return null;
			side = dz > 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
			break;
		case WEST: case EAST:
			d = dx / dir.x;
			if (d < 0 || d > 1.0 || Math.abs(dy - dir.y * d) > height || Math.abs(dz - dir.z * d) > width)
				return null;
			side = dx > 0 ? EnumFacing.WEST : EnumFacing.EAST;
			break;
		default: return null;
		}
		return Pair.of(dir.scale(d), side);
	}

}
