package cd4017be.rs_ctr.api.interact;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.render.Util;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;
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
@SideOnly(Side.CLIENT)
public class InteractiveDeviceRenderer extends HybridFastTESR<TileEntity> {

	@Override
	public void renderTileEntityFast(TileEntity te, double x, double y, double z, float t, int destroy, float alpha, BufferBuilder buffer) {
		World world = te.getWorld();
		BlockPos pos = te.getPos();
		int l = world.getCombinedLight(pos, 0);
		for (IInteractiveComponent c : ((IInteractiveDevice)te).getComponents())
			c.draw(world, pos, x, y, z, l, buffer);
	}

	@Override
	protected void renderSpecialPart(TileEntity te, double x, double y, double z, float t, int destroy, float alpha) {
		Triple<IInteractiveComponent, Vec3d, EnumFacing> r = ((IInteractiveDevice)te).rayTrace(rendererDispatcher.entity, t);
		if (r == null) return;
		Pair<Vec3d, String> text = r.getLeft().getDisplayText(r.getMiddle());
		if (text == null) return;
		GlStateManager.pushMatrix();
		Orientation o = Orientation.fromFacing(r.getRight());
		if (o.ordinal() >= 4) {
			int i = Math.floorMod(Math.round(Minecraft.getMinecraft().player.rotationYaw / 90), 4);
			o = Orientation.values()[i + o.ordinal()];
		}
		Vec3d p = text.getKey();
		Util.moveAndOrientToBlock(x - 0.5 + p.x, y - 0.5 + p.y, z - 0.5 + p.z, o);
		float scale = 1F/128F;
		GlStateManager.scale(-scale, -scale, -scale);
		Util.renderToolTip(Minecraft.getMinecraft().fontRenderer, 0, -16, 0xffffff, 0x80000000, text.getValue().split("\n"));
		GlStateManager.popMatrix();
	}

}
