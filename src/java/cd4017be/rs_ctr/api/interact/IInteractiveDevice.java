package cd4017be.rs_ctr.api.interact;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * 
 * @author CD4017BE
 */
public interface IInteractiveDevice {

	IInteractiveComponent[] getComponents();

	default Triple<IInteractiveComponent, Vec3d, EnumFacing> rayTrace(Entity player, float t) {
		Vec3d p = player.getPositionEyes(t);
		Vec3d d = player.getLook(t).scale(16);
		if (this instanceof TileEntity) {
			BlockPos pos = ((TileEntity)this).getPos();
			p = p.subtract(pos.getX(), pos.getY(), pos.getZ());
		} else if (this instanceof Entity) {
			Entity e = (Entity)this;
			p = p.subtract(e.posX, e.posY, e.posZ)
				.rotateYaw(e.rotationYaw).rotatePitch(e.rotationPitch);
			d = d.rotateYaw(e.rotationYaw).rotatePitch(e.rotationPitch);
		}
		IInteractiveComponent hit = null;
		EnumFacing s = null;
		for (IInteractiveComponent c : getComponents()) {
			Pair<Vec3d, EnumFacing> d1 = c.rayTrace(p, d);
			if (d1 != null) {
				d = d1.getLeft();
				s = d1.getRight();
				hit = c;
			}
		}
		return hit == null ? null : Triple.of(hit, p.add(d), s);
	}

}