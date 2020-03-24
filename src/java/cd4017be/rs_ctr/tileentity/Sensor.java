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
import cd4017be.api.rs_ctr.sensor.IBlockSensor.IHost;
import cd4017be.api.rs_ctr.sensor.SensorRegistry;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * @author CD4017BE
 *
 */
public class Sensor extends WallMountGate implements BlockHandler, SignalHandler, IInteractiveComponent, IBlockRenderComp, IHost {

	protected SignalHandler out;
	protected BlockReference blockRef;
	protected int clock, value, ref;
	protected boolean refChanged;
	protected IBlockSensor impl = SensorRegistry.DEFAULT;
	protected ItemStack stack = ItemStack.EMPTY;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setLocation(0.25F, 0.125F, 0.125F, EnumFacing.WEST).setName("port.rs_ctr.bi"),
			new MountedPort(this, 1, SignalHandler.class, false).setLocation(0.25F, 0.5F, 0.125F, EnumFacing.WEST).setName("port.rs_ctr.clk"),
			new MountedPort(this, 2, SignalHandler.class, true).setLocation(0.75F, 0.5F, 0.125F, EnumFacing.EAST).setName("port.rs_ctr.o"),
			new MountedPort(this, 3, SignalHandler.class, false).setLocation(0.25F, 0.875F, 0.125F, EnumFacing.WEST).setName("port.rs_ctr.ref")
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		return pin == 3 ? (SignalHandler)(v)-> {
			if (v == ref) return;
			ref = v;
			refChanged = true;
			markDirty(SAVE);
		} : this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
		if (out != null) out.updateSignal(value - ref);
	}

	@Override
	protected void resetPin(int pin) {
		if (pin == 0)
			updateBlock(new BlockReference(world, pos.offset(o.front), o.back));
		else if (pin == 3) ref = 0;
	}

	@Override
	public void updateBlock(BlockReference ref) {
		impl.onRefChange(blockRef = ref, this);
		updateSignal(clock);
	}

	@Override
	public void updateSignal(int val) {
		if (val == clock && ((MountedPort)ports[1]).getConnector() != null) return;
		clock = val;
		if (blockRef == null || !blockRef.isLoaded()) return;
		if ((val = impl.readValue(blockRef)) == value && !refChanged) return;
		value = val;
		refChanged = false;
		if (out != null) out.updateSignal(val - ref);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode == SAVE) {
			nbt.setInteger("clk", clock);
			nbt.setInteger("val", value);
			nbt.setInteger("ref", ref);
		}
		if (!stack.isEmpty()) {
			nbt.setTag("sensor", stack.writeToNBT(new NBTTagCompound()));
			if (mode != SAVE && impl instanceof INBTSerializable)
				nbt.setTag("sync", ((INBTSerializable<?>)impl).serializeNBT());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			clock = nbt.getInteger("clk");
			value = nbt.getInteger("val");
			ref = nbt.getInteger("ref");
			refChanged = true;
			blockRef = null;
		}
		stack = nbt.hasKey("sensor", NBT.TAG_COMPOUND) ? new ItemStack(nbt.getCompoundTag("sensor")) : ItemStack.EMPTY;
		impl = SensorRegistry.get(stack);
		if (mode == SAVE)
			impl.onRefChange(blockRef, this);
		else if (impl instanceof INBTSerializable)
			((INBTSerializable<NBTBase>)impl).deserializeNBT(nbt.getTag("sync"));
	}

	@Override
	protected void orient(Orientation o) {
		super.orient(o);
		mountPos = o.rotate(new Vec3d(0, 0, -0.25)).addVector(0.5, 0.5, 0.5);
		if (((MountedPort)ports[0]).getConnector() == null && world != null)
			resetPin(0);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(this);
	}

	Vec3d mountPos = Vec3d.ZERO;

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		boolean rot = o.ordinal() >= 4 && (o.ordinal() & 1) != 0;
		return IInteractiveComponent.rayTraceFlat(start, dir, mountPos, o.back, rot ? 0.5F : 0.25F, rot ? 0.25F : 0.5F);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (!stack.isEmpty() && !player.isSneaking()) {
			IBlockSensor sensor = SensorRegistry.get(stack);
			if (sensor == SensorRegistry.DEFAULT) return false;
			if (!this.stack.isEmpty()) remove(player);
			this.stack = player.isCreative() ? ItemHandlerHelper.copyStackWithSize(stack, 1) : stack.splitStack(1);
			impl = sensor;
			impl.onRefChange(blockRef, this);
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
		if (player == null) ItemFluidUtil.dropStack(stack, world, pos);
		else if (!player.isCreative()) ItemFluidUtil.dropStack(stack, player);
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

	@Override
	public void syncSensorState() {
		markDirty(SYNC);
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		impl.onRefChange(null, null);
	}

	@Override
	public void onLoad() {
		super.onLoad();
	}

	@Override
	public Object getState(int id) {
		switch(id) {
		case 0: return blockRef;
		case 1: return clock;
		case 2: return value - ref;
		default: return ref;
		}
	}

}
