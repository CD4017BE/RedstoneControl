package cd4017be.rs_ctr.render;

import cd4017be.api.rs_ctr.frame.IFrame;
import cd4017be.api.rs_ctr.frame.IFrameOperator;
import static cd4017be.rs_ctr.ClientProxy.t_frame;
import static cd4017be.rs_ctr.ClientProxy.t_beam;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.animation.FastTESR;
import static java.lang.Float.floatToIntBits;

/** @author cd4017be */
public class FrameRenderer extends FastTESR<TileEntity> {

	public static final FrameRenderer FRAME_RENDER = new FrameRenderer();

	@Override
	public void renderTileEntityFast(
		TileEntity te, double x, double y, double z, float partialTicks,
		int destroyStage, float partial, BufferBuilder buffer
	) {
		if(!(te instanceof IFrame)) return;
		{
			BlockPos p = te.getPos();
			x -= p.getX();
			y -= p.getY();
			z -= p.getZ();
		}
		World world = te.getWorld();
		for(BlockPos pos : ((IFrame)te).getLinks()) {
			TileEntity te1 = world.getTileEntity(pos);
			if(te1 instanceof IFrameOperator)
				((IFrameOperator)te1).render(
					x + pos.getX(), y + pos.getY(), z + pos.getZ(), buffer
				);
		}
	}

	public static void renderFrame(
		double x0, double y0, double z0, double x1, double y1, double z1,
		BufferBuilder buf, int color
	) {
		int[] vb = new int[28];
		for (int i = 0; i < 28; i+=7) {
			vb[i + 3] = color;
			vb[i + 6] = 0xf000f0;
		}
		vb[4] = vb[11] = floatToIntBits(t_frame.getMinU());
		vb[18] = vb[25] = floatToIntBits(t_frame.getMaxU());
		vb[5] = vb[26] = floatToIntBits(t_frame.getMinV());
		vb[12] = vb[19] = floatToIntBits(t_frame.getMaxV());
		int X0 = floatToIntBits((float)x0), X1 = floatToIntBits((float)x1);
		int Y0 = floatToIntBits((float)y0), Y1 = floatToIntBits((float)y1);
		int Z0 = floatToIntBits((float)z0), Z1 = floatToIntBits((float)z1);
		if(x0 < x1 && z0 < z1) {
			vb[0] = vb[7] = X0;
			vb[14] = vb[21] = X1;
			vb[2] = vb[23] = Z0;
			vb[9] = vb[16] = Z1;
			vb[1] = vb[8] = vb[15] = vb[22] = Y0;
			buf.addVertexData(vb);
			vb[1] = vb[8] = vb[15] = vb[22] = Y1;
			buf.addVertexData(vb);
		}
		if(x0 < x1 && y0 < y1) {
			vb[0] = vb[7] = X0;
			vb[14] = vb[21] = X1;
			vb[1] = vb[22] = Y0;
			vb[8] = vb[15] = Y1;
			vb[2] = vb[9] = vb[16] = vb[23] = Z0;
			buf.addVertexData(vb);
			vb[2] = vb[9] = vb[16] = vb[23] = Z1;
			buf.addVertexData(vb);
		}
		if(y0 < y1 && z0 < z1) {
			vb[1] = vb[8] = Y0;
			vb[15] = vb[22] = Y1;
			vb[2] = vb[23] = Z0;
			vb[9] = vb[16] = Z1;
			vb[0] = vb[7] = vb[14] = vb[21] = X0;
			buf.addVertexData(vb);
			vb[0] = vb[7] = vb[14] = vb[21] = X1;
			buf.addVertexData(vb);
		}
	}

	public static void renderBeam(
		double x, double y, double z, int dx, int dy, int dz,
		EnumFacing side, BufferBuilder buf, int color
	) {
		final double d = .5625;
		double x1 = x + dx;
		double y1 = y + dy;
		double z1 = z + dz;
		renderFrame(x1 - d, y1 - d, z1 - d, x1 + d, y1 + d, z1 + d, buf, color);
		x1 += side.getFrontOffsetX() * .5;
		y1 += side.getFrontOffsetY() * .5;
		z1 += side.getFrontOffsetZ() * .5;
		int[] vb = new int[28];
		for (int i = 0; i < 28; i+=7) {
			vb[i + 3] = color;
			vb[i + 6] = 0xf000f0;
		}
		vb[4] = vb[11] = floatToIntBits(t_beam.getMinU());
		vb[18] = vb[25] = floatToIntBits(t_beam.getMaxU());
		vb[5] = vb[26] = floatToIntBits(t_beam.getMinV());
		vb[12] = vb[19] = floatToIntBits(t_beam.getMaxV());
		vb[0] = floatToIntBits((float)x);
		vb[1] = floatToIntBits((float)y);
		vb[2] = floatToIntBits((float)z);
		vb[7] = vb[14] = vb[21] = floatToIntBits((float)x1);
		vb[8] = vb[15] = vb[22] = floatToIntBits((float)y1);
		vb[9] = vb[16] = vb[23] = floatToIntBits((float)z1);
		int i, j;
		switch(side.getAxis()) {
		case X:
			vb[8] = floatToIntBits((float)(y1 - d));
			vb[22] = floatToIntBits((float)(y1 + d));
			vb[9] = i = floatToIntBits((float)(z1 - d));
			vb[23] = j = floatToIntBits((float)(z1 + d));
			buf.addVertexData(vb);
			vb[9] = j;
			vb[23] = i;
			buf.addVertexData(vb);
			return;
		case Y:
			vb[7] = floatToIntBits((float)(x1 - d));
			vb[21] = floatToIntBits((float)(x1 + d));
			vb[9] = i = floatToIntBits((float)(z1 - d));
			vb[23] = j = floatToIntBits((float)(z1 + d));
			buf.addVertexData(vb);
			vb[9] = j;
			vb[23] = i;
			buf.addVertexData(vb);
			return;
		case Z:
			vb[7] = floatToIntBits((float)(x1 - d));
			vb[21] = floatToIntBits((float)(x1 + d));
			vb[8] = i = floatToIntBits((float)(y1 - d));
			vb[22] = j = floatToIntBits((float)(y1 + d));
			buf.addVertexData(vb);
			vb[8] = j;
			vb[22] = i;
			buf.addVertexData(vb);
			return;
		}
	}

}
