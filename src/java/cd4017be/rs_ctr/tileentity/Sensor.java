package cd4017be.rs_ctr.tileentity;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.api.rs_ctr.sensor.SensorRegistry;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public class Sensor extends WallMountGate implements BlockHandler, SignalHandler, IInteractiveComponent, IBlockRenderComp {

	protected SignalHandler out;
	protected BlockReference blockRef;
	protected int clock, value;
	protected IBlockSensor impl = SensorRegistry.DEFAULT;
	protected ItemStack stack = ItemStack.EMPTY;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setLocation(0.25F, 0.25F, 0.125F, EnumFacing.WEST).setName("port.rs_ctr.bi"),
			new MountedPort(this, 1, SignalHandler.class, false).setLocation(0.25F, 0.75F, 0.125F, EnumFacing.WEST).setName("port.rs_ctr.clk"),
			new MountedPort(this, 2, SignalHandler.class, true).setLocation(0.75F, 0.5F, 0.125F, EnumFacing.EAST).setName("port.rs_ctr.o")
		};
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null) out.updateSignal(value);
	}

	@Override
	protected void resetPin(int pin) {
		if (pin == 0) blockRef = null;
		else clock = 0;
	}

	@Override
	public void updateBlock(BlockReference ref) {
		blockRef = ref;
	}

	@Override
	public void updateSignal(int val) {
		if (val == clock) return;
		clock = val;
		if (blockRef == null || !blockRef.isLoaded()) return;
		if ((val = impl.readValue(blockRef)) == value) return;
		value = val;
		if (out != null) out.updateSignal(val);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode == SAVE) {
			nbt.setInteger("clk", clock);
			nbt.setInteger("val", value);
		}
		if (!stack.isEmpty())
			nbt.setTag("sensor", stack.writeToNBT(new NBTTagCompound()));
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			clock = nbt.getInteger("clk");
			value = nbt.getInteger("val");
			blockRef = null;
		}
		stack = nbt.hasKey("sensor", NBT.TAG_COMPOUND) ? new ItemStack(nbt.getCompoundTag("sensor")) : ItemStack.EMPTY;
		impl = SensorRegistry.get(stack);
	}

	@Override
	protected void orient(Orientation o) {
		super.orient(o);
		mountPos = o.rotate(new Vec3d(0, 0, -0.25)).addVector(0.5, 0.5, 0.5);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(this);
	}

	Vec3d mountPos = Vec3d.ZERO;

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		return IInteractiveComponent.rayTraceFlat(start, dir, mountPos, o.back, 0.25F, 0.5F);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (!stack.isEmpty() && !player.isSneaking()) {
			IBlockSensor sensor = SensorRegistry.get(stack);
			if (sensor == SensorRegistry.DEFAULT) return false;
			if (!this.stack.isEmpty()) remove(player);
			this.stack = stack.splitStack(1);
			impl = sensor;
			markDirty(REDRAW);
			return true;
		} else if (hit || player.isSneaking() && stack.isEmpty()) {
			if (this.stack.isEmpty()) return false;
			remove(player);
			markDirty(REDRAW);
			return true;
		}
		return false;
	}

	private void remove(EntityPlayer player) {
		if (player != null)
			ItemFluidUtil.dropStack(stack, player);
		else ItemFluidUtil.dropStack(stack, world, pos);
		stack = ItemStack.EMPTY;
		impl = SensorRegistry.DEFAULT;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		return Pair.of(mountPos, impl.getTooltipString());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		if (stack.isEmpty()) return;
		ResourceLocation model = impl.getModel();
		if (model != null)
			PortRenderer.PORT_RENDER.drawModel(quads, (float)mountPos.x, (float)mountPos.y, (float)mountPos.z, o, model.toString());
	}

}
