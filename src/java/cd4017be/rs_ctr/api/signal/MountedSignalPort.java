package cd4017be.rs_ctr.api.signal;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.api.signal.IConnector.IConnectorItem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
public class MountedSignalPort extends SignalPort implements IInteractiveComponent {

	/**radius of the hit-box */
	public static final float SIZE = 0.125F;

	/**port name */
	public String name = "";
	/**port location relative to the owner */
	public Vec3d pos;
	/**the side from which wires are connected */
	public EnumFacing face;
	/**the connection attached to this port */
	protected IConnector connector;

	/**
	 * @param owner
	 * @param pin
	 * @param isSource
	 */
	public MountedSignalPort(ISignalIO owner, int pin, boolean isSource) {
		super(owner, pin, isSource);
	}

	/**
	 * @param x relative x
	 * @param y relative y
	 * @param z relative z
	 * @param face attachment side
	 * @return this
	 */
	public MountedSignalPort setLocation(double x, double y, double z, EnumFacing face) {
		this.pos = new Vec3d(x, y, z);
		this.face = face;
		return this;
	}

	/**
	 * @param x relative x
	 * @param y relative y
	 * @param z relative z
	 * @param face attachment side
	 * @param o rotation of the whole system
	 * @return this
	 */
	public MountedSignalPort setLocation(double x, double y, double z, EnumFacing face, Orientation o) {
		this.pos = o.rotate(new Vec3d(x - 0.5F, y - 0.5F, z - 0.5F)).addVector(0.5, 0.5, 0.5);
		this.face = o.rotate(face);
		return this;
	}

	/**
	 * @param name port name
	 * @return this
	 */
	public MountedSignalPort setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		String s = TooltipUtil.translate(name);
		if (linkID != 0) s += "\nID " + linkID;
		if (connector != null) s += connector.displayInfo(this);
		return Pair.of(pos, s);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void draw(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (connector != null) connector.renderConnection(world, pos, this, x + this.pos.x, y + this.pos.y, z + this.pos.z, light, buffer);
	}

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		return IInteractiveComponent.rayTraceFlat(start, dir, pos, face, SIZE, SIZE);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (hit || player.isCreative() && player.isSneaking() && stack.isEmpty()) {
			setConnector(null, player);
			return true;
		}
		if (stack.getItem() instanceof IConnectorItem) {
			((IConnectorItem)stack.getItem()).doAttach(stack, this, player);
			return true;
		}
		return false;
	}

	public IConnector getConnector() {
		return connector;
	}

	/**
	 * @param c new connector
	 * @param by player responsible for change
	 */
	public void setConnector(@Nullable IConnector c, @Nullable EntityPlayer by) {
		if (connector == c) return;
		IConnector old = connector; connector = c;
		int ev = 0;
		if (old != null) {
			old.onUnload();
			old.onRemoved(this, by);
			ev |= ISignalIO.E_CON_REM;
		}
		if (c != null) {
			c.onLoad(this);
			ev |= ISignalIO.E_CON_ADD;
		}
		owner.onPortModified(this, ev);
	}

	/**
	 * @return whether this signal port may move around (because its mounted on a moving entity for example).<br>
	 * This is used to determine whether it can be hooked on a connection that has fixed/limited range.
	 */
	public boolean canMove() {
		return false;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		if (connector != null)
			nbt.setTag("con", connector.serializeNBT());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		connector = IConnector.load(nbt.getCompoundTag("con"));
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (connector != null) connector.onLoad(this);
	}

	@Override
	public void onUnload() {
		super.onUnload();
		if (connector != null) connector.onUnload();
	}

}
