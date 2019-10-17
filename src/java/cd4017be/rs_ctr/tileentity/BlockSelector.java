package cd4017be.rs_ctr.tileentity;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.frame.IFrameOperator;
import static cd4017be.api.rs_ctr.frame.IFrameOperator.*;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.render.Util;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.gui.BlockButton;
import cd4017be.rs_ctr.render.FrameRenderer;
import cd4017be.rs_ctr.render.ISpecialRenderComp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author cd4017be */
public class BlockSelector extends WallMountGate
implements IFrameOperator, IUpdatable, IntConsumer,
Supplier<String>, ISpecialRenderComp {

	public static int RANGE = 64;
	BlockButton scanBtn = new BlockButton(this, () -> null, this)
	.setSize(0.25F, 0.25F);
	public int[] area = new int[6];
	public byte missingFrames = -1;
	BlockHandler out;
	public int sx, sy, sz;
	byte tick;
	boolean delayed;
	boolean showFrame = true;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, SignalHandler.class, false)
			.setName("port.rs_ctr.x")
			.setLocation(.875, .125, 0, EnumFacing.NORTH),
			new MountedPort(this, 1, SignalHandler.class, false)
			.setName("port.rs_ctr.y")
			.setLocation(.625, .125, 0, EnumFacing.NORTH),
			new MountedPort(this, 2, SignalHandler.class, false)
			.setName("port.rs_ctr.z")
			.setLocation(.375, .125, 0, EnumFacing.NORTH),
			new MountedPort(this, 3, BlockHandler.class, true)
			.setName("port.rs_ctr.bo")
			.setLocation(.125, .125, 0, EnumFacing.NORTH)
		};
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		switch(pin) {
		case 0:
			return (val) -> {
				if(val == sx) return;
				onInputChange();
				sx = val;
				markDirty(SYNC);
			};
		case 1:
			return (val) -> {
				if(val == sy) return;
				onInputChange();
				sy = val;
				markDirty(SYNC);
			};
		case 2:
			return (val) -> {
				if(val == sz) return;
				onInputChange();
				sz = val;
				markDirty(SYNC);
			};
		default:
			return null;
		}
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if(callback instanceof BlockHandler) {
			out = (BlockHandler)callback;
			out.updateBlock(selBlock());
		} else out = null;
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).updateSignal(0);
	}

	private void onInputChange() {
		if(tick == 0) {
			tick = TickRegistry.TICK;
			TickRegistry.schedule(this);
		} else if(tick != TickRegistry.TICK) {
			process();
			tick = TickRegistry.TICK;
			delayed = true;
		}
	}

	@Override
	public void process() {
		if(delayed) {
			delayed = false;
			TickRegistry.schedule(this);
			return;
		}
		tick = 0;
		if(out != null)
			out.updateBlock(selBlock());
	}

	private BlockReference selBlock() {
		if(
			missingFrames != 0 || sx < 0 || sy < 0 || sz < 0
			|| sx >= area[3] || sy >= area[4] || sz >= area[5]
		) return null;
		return new BlockReference(
			world, new BlockPos(sx + area[0], sy + area[1], sz + area[2]),
			getOrientation().front
		);
	}

	@Override
	public void onFrameBreak(BlockPos pos) {
		int i = 0, d;
		d = pos.getX() - area[0];
		if(d == area[3]) i |= 1;
		else if(d != -1) return;
		d = pos.getY() - area[1];
		if(d == area[4]) i |= 2;
		else if(d != -1) return;
		d = pos.getZ() - area[2];
		if(d == area[4]) i |= 4;
		else if(d != -1) return;
		missingFrames |= 1 << i;
		markDirty(SYNC);
	}

	@Override
	public String get() {
		if(Minecraft.getMinecraft().player.isSneaking())
			return TooltipUtil
			.translate("port.rs_ctr.show_sel" + (showFrame ? '1' : '0'));
		int dx = area[3], dy = area[4], dz = area[5];
		char status = dx <= 0 || dy <= 0 || dz <= 0 ? '2'
			: missingFrames != 0 ? '1' : '0';
		return TooltipUtil.format(
			"port.rs_ctr.area" + status, dx, dy, dz,
			Integer.bitCount(missingFrames & 0xff)
		);
	}

	@Override
	public void accept(int value) {
		if((value & BlockButton.A_HIT) != 0) return;
		if((value & BlockButton.A_SNEAKING) == 0) {
			unlinkCorners(world, pos, area, ~missingFrames);
			scanArea(world, pos, area, RANGE, getOrientation().front);
			missingFrames = (byte)checkCorners(world, pos, area);
		} else showFrame = !showFrame;
		markDirty(SYNC);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		nbt.setInteger("sx", sx);
		nbt.setInteger("sy", sy);
		nbt.setInteger("sz", sz);
		nbt.setIntArray("area", area);
		nbt.setByte("frame", missingFrames);
		nbt.setBoolean("dsp", showFrame);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		sx = nbt.getInteger("sx");
		sy = nbt.getInteger("sy");
		sz = nbt.getInteger("sz");
		int[] arr = nbt.getIntArray("area");
		System.arraycopy(arr, 0, area, 0, Math.min(arr.length, 6));
		missingFrames = nbt.getByte("frame");
		showFrame = nbt.getBoolean("dsp");
		super.loadState(nbt, mode);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		unlinkCorners(world, pos, area, ~missingFrames);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(scanBtn);
	}

	@Override
	protected void orient(Orientation o) {
		scanBtn.setLocation(0.5, 0.75, 0, o);
		super.orient(o);
	}

	@SideOnly(Side.CLIENT)
	private int lastRender;

	@Override
	@SideOnly(Side.CLIENT)
	public void render(double x, double y, double z, BufferBuilder buffer) {
		if(Util.RenderFrame == lastRender) return;
		lastRender = Util.RenderFrame;
		if(
			missingFrames == 0 && sx >= 0 && sy >= 0 && sz >= 0
			&& sx < area[3] && sy < area[4] && sz < area[5]
		) FrameRenderer.renderBeam(
			x + .5, y + .5, z + .5,
			sx + area[0] - pos.getX(),
			sy + area[1] - pos.getY(),
			sz + area[2] - pos.getZ(),
			getOrientation().front, buffer, 0x7fffff00
		);
		if(showFrame) {
			x += area[0] - pos.getX();
			y += area[1] - pos.getY();
			z += area[2] - pos.getZ();
			FrameRenderer.renderFrame(
				x - .0625, y - .0625, z - .0625,
				x + (double)area[3] + .0625,
				y + (double)area[4] + .0625,
				z + (double)area[5] + .0625,
				buffer, missingFrames == 0 ? 0x7f00ff00 : 0x7f0000ff
			);
		}
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

}
