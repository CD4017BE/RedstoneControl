package cd4017be.rs_ctr.tileentity;

import java.util.HashMap;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import cd4017be.api.rs_ctr.frame.IFrameOperator;
import static cd4017be.api.rs_ctr.frame.IFrameOperator.*;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.util.DimPos;
import cd4017be.lib.util.MovedBlock;
import cd4017be.lib.util.Orientation;
import static cd4017be.lib.util.TooltipUtil.translate;
import static cd4017be.lib.util.TooltipUtil.format;
import cd4017be.rs_ctr.gui.BlockButton;
import cd4017be.rs_ctr.render.FrameRenderer;
import cd4017be.rs_ctr.render.ISpecialRenderComp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author cd4017be */
public class StructTeleporter extends Teleporter
implements IFrameOperator, IntConsumer, Supplier<String>, ISpecialRenderComp, ITESRenderComp {

	static final int INNER_AREA = 2, OUTER_FRAME = 4, INVALID_FRAME = -3;
	public static int RANGE = 64;
	BlockButton button = new BlockButton(this, () -> null, this).setSize(0.5F, 0.25F);
	public int[] area = new int[6];
	public byte missingFrames = -1;
	boolean showFrame = true;

	{
		ports[0].setLocation(.875, .125, 0, EnumFacing.NORTH);
		ports[1].setLocation(.125, .125, 0, EnumFacing.NORTH);
		ports[2].setLocation(.625, .125, 0, EnumFacing.NORTH);
		ports[3].setLocation(.375, .125, 0, EnumFacing.NORTH);
		ports[4].setLocation(.5, .375, 0, EnumFacing.NORTH);
	}

	@Override
	protected int run() {
		if (action == SINGLE_BLOCK) return super.run();
		if (missingFrames != 0 || area[3] <= 0 || area[4] <= 0 || area[5] <= 0) return INVALID_FRAME;
		if (dest == null || ref == null || ref.dim != world.provider.getDimension() || !isInArea(ref.pos)) return INVALID_COORDS;
		double distance = Math.sqrt(new DimPos(ref.pos, ref.dim).distanceSq(new DimPos(dest.pos, dest.dim)));
		if (distance == 0) return COMPLETE;
		if (distance > MAX_DISTANCE) distance = MAX_DISTANCE;
		
		DimPos pa, pb;
		BlockPos size;
		boolean skipInner;
		if ((action & OUTER_FRAME) != 0) {
			skipInner = (action & INNER_AREA) == 0;
			pa = new DimPos(area[0] - 1, area[1], area[2] - 1, ref.dim);
			size = new BlockPos(area[3] + 2, area[4], area[5] + 2);
		} else {
			skipInner = false;
			pa = new DimPos(area[0], area[1], area[2], ref.dim);
			size = new BlockPos(area[3], area[4], area[5]);
		}
		pb = new DimPos(dest.pos.subtract(ref.pos).add(pa), dest.dim);
		if (!(
			world.isValid(pa) && world.isValid(pb)
			&& world.isValid(pa.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1))
			&& world.isValid(pb.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1))
		)) return OUT_OF_WORLD;
		
		int count = 0, inner = area[3] * area[5];
		if ((action & INNER_AREA) != 0) count += inner;
		if ((action & OUTER_FRAME) != 0) count += (area[3] + 1) * (area[5] + 1) - inner;
		count *= area[4];
		long needed = Math.round((double)count * distance * ENERGY_PER_BLOCK);
		return initTeleport(
			()-> {
				if (unloaded) return;
				buffer -= needed;
				swapArea(pa, pb, size, skipInner);
				List<Entity> entitiesA = getEntities(pa, size, skipInner);
				List<Entity> entitiesB = getEntities(pb, size, skipInner);
				moveEntities(entitiesA, pa, pb);
				moveEntities(entitiesB, pb, pa);
			}, needed
		);
	}

	private boolean isInArea(BlockPos pos) {
		int i = pos.getX() - area[0];
		if (i < -1 || i > area[3]) return false;
		i = pos.getY() - area[1];
		if (i < 0 || i >= area[4]) return false;
		i = pos.getZ() - area[2];
		if (i < -1 || i > area[5]) return false;
		return true;
	}

	private static void swapArea(DimPos pa, DimPos pb, BlockPos size, boolean skipInner) {
		int rx = size.getX(), ry = size.getY(), rz = size.getZ();
		int dx = 1, dy = 1, dz = 1;
		if (pb.getX() < pa.getX()) {
			pa = pa.add(rx, 0, 0);
			pb = pb.add(rx, 0, 0);
			rx = -rx;
			dx = -1;
		} else {
			pa = pa.add(-1, 0, 0);
			pb = pb.add(-1, 0, 0);
		}
		if (pb.getY() < pa.getY()) {
			pa = pa.add(0, ry, 0);
			pb = pb.add(0, ry, 0);
			ry = -ry;
			dy = -1;
		} else {
			pa = pa.add(0, -1, 0);
			pb = pb.add(0, -1, 0);
		}
		if (pb.getZ() < pa.getZ()) {
			pa = pa.add(0, 0, rz);
			pb = pb.add(0, 0, rz);
			rz = -rz;
			dz = -1;
		} else {
			pa = pa.add(0, 0, -1);
			pb = pb.add(0, 0, -1);
		}
		HashMap<DimPos, NBTTagCompound> tiles = new HashMap<>();
		for (int x = rx; x != 0; x -= dx)
			for (int z = rz; z != 0; z -= dz) {
				if (skipInner && x != dx && z != dz && x != rx && z != rz) continue;
				for (int y = ry; y != 0; y -= dy)
					swap(pa.add(x, y, z), pb.add(x, y, z), tiles);
			}
		MovedBlock.addTileEntities(tiles);
	}

	@Override
	public void onFrameBreak(BlockPos pos) {
		int i = 0, d;
		d = pos.getX() - area[0];
		if(d == area[3]) i |= 1;
		else if(d != -1) return;
		d = pos.getY() - area[1];
		if(d == area[4] - 1) i |= 2;
		else if(d != 0) return;
		d = pos.getZ() - area[2];
		if(d == area[5]) i |= 4;
		else if(d != -1) return;
		missingFrames |= 1 << i;
		markDirty(SYNC);
	}

	@Override
	public String get() {
		if(Minecraft.getMinecraft().player.isSneaking())
			return translate("port.rs_ctr.show_sel" + (showFrame ? '1' : '0'));
		int dx = area[3], dy = area[4], dz = area[5];
		char status = dx <= 0 || dy <= 0 || dz <= 0 ? '2'
			: missingFrames != 0 ? '1' : '0';
		return format(
			"port.rs_ctr.area" + status, dx, dy, dz,
			Integer.bitCount(missingFrames & 0xff)
		);
	}

	@Override
	public void accept(int value) {
		if((value & BlockButton.A_HIT) != 0) return;
		if((value & BlockButton.A_SNEAKING) == 0) {
			if (
				area[3] <= 0 || area[4] <= 0 || area[5] <= 0
				|| (missingFrames = (byte)checkCorners(world, pos, area)) != 0
			) {
				unlinkCorners(world, pos, area, ~missingFrames);
				scanArea(world, pos, area, RANGE, getOrientation().front);
				missingFrames = (byte)checkCorners(world, pos, area);
			}
		} else showFrame = !showFrame;
		markDirty(SYNC);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		writeArea(area, nbt, pos);
		nbt.setByte("frame", missingFrames);
		nbt.setBoolean("dsp", showFrame);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		readArea(area, nbt, pos);
		missingFrames = nbt.getByte("frame");
		showFrame = nbt.getBoolean("dsp");
	}

	@Override
	public void invalidate() {
		super.invalidate();
		unlinkCorners(world, pos, area, ~missingFrames);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(button);
	}

	@Override
	protected void orient(Orientation o) {
		super.orient(o);
		button.setLocation(0.5, 0.75, 0, o);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderSpecial(
		double x, double y, double z, float t, FontRenderer fr
	) {
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		int mf = missingFrames & 0xff;
		if (mf == 0) return;
		GlStateManager.glLineWidth(2.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.disableDepth();
		for (int i = 0; i < 8; i++, mf >>= 1) {
			if ((mf & 1) == 0) continue;
			BlockPos p = getCorner(area, i).subtract(pos);
			double x1 = x + p.getX();
			double y1 = y + p.getY();
			double z1 = z + p.getZ();
			RenderGlobal.drawBoundingBox(
				x1, y1, z1, x1 + 1., y1 + 1., z1 + 1,
				1, 0, 0, 1
			);
		}
		GlStateManager.enableDepth();
		GlStateManager.enableTexture2D();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasFastRenderer() {
		return missingFrames == 0 && !HybridFastTESR.isAimedAt(this);
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		int d;
		return super.getMaxRenderDistanceSquared()
			+ (d = area[3]) * d
			+ (d = area[4]) * d
			+ (d = area[5]) * d;
	}

	@Override
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if(showFrame) {
			x += area[0] - pos.getX();
			y += area[1] - pos.getY();
			z += area[2] - pos.getZ();
			FrameRenderer.renderFrame(
				x - .0625, y - .0625, z - .0625,
				x + (double)area[3] + .0625,
				y + (double)area[4] + .0625,
				z + (double)area[5] + .0625,
				buffer, missingFrames == 0 ? 0x7fff007f : 0x7f0000ff
			);
		}
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return new AxisAlignedBB(
			area[0] - 1, area[1], area[2] - 1,
			area[0] + area[3] + 1, area[1] + area[4], area[2] + area[5] + 1
		);
	}

}
