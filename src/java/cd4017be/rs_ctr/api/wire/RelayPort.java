package cd4017be.rs_ctr.api.wire;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import cd4017be.rs_ctr.api.wire.IWiredConnector.IWiredConnectorItem;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A double SignalPort that rather than providing a functional connection on it's own, only passes it further to other ports.<br>
 * Used to implement wire anchors.
 * @author cd4017be
 */
public class RelayPort extends MountedSignalPort implements IBlockRenderComp {

	public static final float SIZE = MountedSignalPort.SIZE / 4F;

	public final RelayPort opposite;

	/**
	 * @param owner
	 * @param pin convention: 0x8??? for source, 0x9??? for sink
	 */
	public RelayPort(ISignalIO owner, int pin) {
		super(owner, pin & 0xfff | 0x8000, true);
		this.opposite = new RelayPort(this);
	}

	private RelayPort(RelayPort opposite) {
		super(opposite.owner, opposite.pin ^ 0x1000, !opposite.isSource);
		this.opposite = opposite;
	}

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		RayTraceResult rt = new AxisAlignedBB(pos.subtract(SIZE, SIZE, SIZE), pos.addVector(SIZE, SIZE, SIZE)).calculateIntercept(start, start.add(dir));
		return rt == null ? null : Pair.of(rt.hitVec.subtract(start), rt.sideHit);
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		String s;
		if (this.connector != null) s = this.connector.displayInfo(this, linkID);
		else if (opposite.connector != null) s = opposite.connector.displayInfo(opposite, linkID);
		else s = TooltipUtil.translate(name);
		if (s.charAt(0) == '\n') s = s.substring(1);
		return Pair.of(pos, s);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		//TODO render pin
	}

	@Override
	public <T> void addRenderComps(List<T> list, Class<T> type) {
		super.addRenderComps(list, type);
		opposite.addRenderComps(list, type);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (hit || player.isSneaking() && stack.isEmpty()) {
			setConnector(null, player);
			return true;
		}
		if (stack.getItem() instanceof IWiredConnectorItem) {
			((IWiredConnectorItem)stack.getItem()).doAttach(stack, this, player);
			return true;
		}
		return false;
	}

	@Override
	public MountedSignalPort setLocation(double x, double y, double z, EnumFacing face) {
		opposite.pos = this.pos = new Vec3d(x, y, z);
		opposite.face = (this.face = face).getOpposite();
		return this;
	}

	@Override
	public MountedSignalPort setLocation(double x, double y, double z, EnumFacing face, Orientation o) {
		super.setLocation(x, y, z, face, o);
		opposite.pos = this.pos;
		opposite.face = this.face.getOpposite();
		return this;
	}

	@Override
	public void connect(SignalPort to) {
		if (!(to instanceof MountedSignalPort)) return;
		MountedSignalPort port = (MountedSignalPort)to;
		SignalLine line = new SignalLine(this);
		if (line.source == null || line.sink == null || !line.contains(port)) return;
		//TODO label
		line.source.connect(line.sink);
		int id = line.source.getLink();
		for (RelayPort rp : line.hooks) {
			rp.linkID = id;
			rp.owner.onPortModified(rp, ISignalIO.E_CONNECT);
		}
	}

	@Override
	public void disconnect() {
		SignalLine line = new SignalLine(this);
		if (line.source != null) line.source.disconnect();
		else if (line.sink != null) line.sink.disconnect();
		for (RelayPort rp : line.hooks) {
			rp.linkID = 0;
			rp.owner.onPortModified(rp, ISignalIO.E_DISCONNECT);
		}
	}

	@Override
	public void onLoad() {
		if (this.connector != null) this.connector.onLoad(this);
		if (opposite.connector != null) opposite.connector.onLoad(this);
	}

	@Override
	public void onUnload() {
		if (this.connector != null) this.connector.onUnload();
		if (opposite.connector != null) opposite.connector.onUnload();
	}

}
