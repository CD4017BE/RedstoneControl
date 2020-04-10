package cd4017be.rs_ctr.tileentity;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.frame.IFrameOperator;
import static cd4017be.api.rs_ctr.frame.IFrameOperator.*;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.render.HybridFastTESR;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author cd4017be */
public class BlockSelector extends WallMountGate
implements IFrameOperator, IUpdatable, IntConsumer,
Supplier<String>, ISpecialRenderComp, ITESRenderComp {

	public static int RANGE = 64;
	BlockButton[] buttons = new BlockButton[4];
	public int[] area = new int[6];
	public byte missingFrames = -1, invertAxis;
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
		for (int i = 0; i < 3; i++) {
			int ax = i;
			buttons[i] = new BlockButton(
				a -> invert(ax),
				()-> "_buttons.num(" + (invertAxis >> ax & 1) + ")",
				()-> translate("port.rs_ctr.invax" + (invertAxis >> ax & 1))
			).setSize(0.0625F, 0.0625F);
		}
		buttons[3] = new BlockButton(this, () -> null, this).setSize(0.5F, 0.25F);
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		switch(pin) {
		case 0:
			return (val) -> {
				if(val == sx) return;
				onInputChange();
				sx = val;
			};
		case 1:
			return (val) -> {
				if(val == sy) return;
				onInputChange();
				sy = val;
			};
		case 2:
			return (val) -> {
				if(val == sz) return;
				onInputChange();
				sz = val;
			};
		default:
			return null;
		}
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if(callback instanceof BlockHandler) {
			out = (BlockHandler)callback;
			out.updateBlock(getOutput());
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
			out.updateBlock(getOutput());
		markDirty(SYNC);
	}

	private BlockReference getOutput() {
		BlockPos p = selBlock();
		return p == null ? null :
			new BlockReference(world, p, getOrientation().front);
	}

	private BlockPos selBlock() {
		if(
			missingFrames != 0 || sx < 0 || sy < 0 || sz < 0
			|| sx >= area[3] || sy >= area[4] || sz >= area[5]
		) return null;
		int x = sx, y = sy, z = sz, inv = invertAxis;
		if ((inv & 1) != 0) x = area[3] - x - 1;
		if ((inv & 2) != 0) y = area[4] - y - 1;
		if ((inv & 4) != 0) z = area[5] - z - 1;
		return new BlockPos(x + area[0], y + area[1], z + area[2]);
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
				fallbackArea();
				missingFrames = (byte)checkCorners(world, pos, area);
			}
		} else showFrame = !showFrame;
		markDirty(SYNC);
	}

	private void fallbackArea() {
		if (area[3] > 0 && area[4] > 0 && area[5] > 0) return;
		if (sx <= 0 || sx > RANGE || sy <= 0 || sy > RANGE || sz <= 0 || sz > RANGE) return;
		area[0] = pos.getX() - ((invertAxis & 1) != 0 ? sx : -1);
		area[1] = pos.getY() - ((invertAxis & 2) != 0 ? sy - 1 : 0);
		area[2] = pos.getZ() - ((invertAxis & 4) != 0 ? sz : -1);
		area[3] = sx;
		area[4] = sy;
		area[5] = sz;
	}

	private void invert(int ax) {
		invertAxis ^= 1 << ax;
		onInputChange();
		markDirty(REDRAW);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		nbt.setInteger("sx", sx);
		nbt.setInteger("sy", sy);
		nbt.setInteger("sz", sz);
		writeArea(area, nbt, pos);
		nbt.setByte("frame", missingFrames);
		nbt.setBoolean("dsp", showFrame);
		nbt.setByte("inv", invertAxis);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		sx = nbt.getInteger("sx");
		sy = nbt.getInteger("sy");
		sz = nbt.getInteger("sz");
		readArea(area, nbt, pos);
		missingFrames = nbt.getByte("frame");
		showFrame = nbt.getBoolean("dsp");
		invertAxis = nbt.getByte("inv");
		super.loadState(nbt, mode);
	}

	@Override
	public void breakBlock() {
		super.breakBlock();
		unlinkCorners(world, pos, area, ~missingFrames);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		Collections.addAll(list, buttons);
	}

	@Override
	protected void orient(Orientation o) {
		Orientation opp = Orientation.values()[o.ordinal() + 2 & 3];
		for (int i = 0; i < 3; i++)
			buttons[i].setLocation(.125 + i * .25, .375, 1, opp);
		buttons[3].setLocation(0.5, 0.75, 0, o);
		super.orient(o);
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
		BlockPos p = selBlock();
		if(p != null) FrameRenderer.renderBeam(
			x + .5, y + .5, z + .5,
			p.getX() - pos.getX(),
			p.getY() - pos.getY(),
			p.getZ() - pos.getZ(),
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
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return new AxisAlignedBB(
			area[0] - 1, area[1], area[2] - 1,
			area[0] + area[3] + 1, area[1] + area[4], area[2] + area[5] + 1
		);
	}

	@Override
	public Object getState(int id) {
		switch(id) {
		case 0: return sx;
		case 1: return sy;
		case 2: return sz;
		default: return getOutput();
		}
	}

}
