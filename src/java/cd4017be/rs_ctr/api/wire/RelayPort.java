package cd4017be.rs_ctr.api.wire;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.ITagableConnector;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import cd4017be.rs_ctr.api.wire.IWiredConnector.IWiredConnectorItem;
import cd4017be.rs_ctr.api.wire.SignalLine.WireLoopException;
import cd4017be.rs_ctr.render.PortRenderer;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A double SignalPort that rather than providing a functional connection on it's own, only passes it further to other ports.<br>
 * Used to implement wire anchors.
 * @author cd4017be
 */
public class RelayPort extends MountedSignalPort implements IBlockRenderComp {

	@ObjectHolder(value = "rs_ctr:wire_anchor")
	public static final Item HOOK_ITEM = null;
	public static final float SIZE = MountedSignalPort.SIZE / 4F;

	public final RelayPort opposite;
	/** the anchor direction for render: 0bZZYYXX, X/Y/Z = {0b01, 0b10, 0b11} */
	public int orient;

	/**
	 * @param owner
	 * @param pin convention: 0x8??? for source, 0x9??? for sink
	 */
	public RelayPort(ISignalIO owner, int pin) {
		super(owner, pin & 0xfff | 0x8000, true);
		this.opposite = new RelayPort(this);
		this.orient = pin >> 16 & 0x3f;
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
		if (!s.isEmpty() && s.charAt(0) == '\n') s = s.substring(1);
		return Pair.of(pos, s);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(quads, (float)pos.x, (float)pos.y, (float)pos.z, Orientation.values()[orient >> 8 & 15], "_hook.pin(" + Integer.toString(orient >> 12 & 3) + ")");
	}

	public void orient(Orientation o) {
		setLocation((double)(pin & 0x3) * 0.25 + 0.125, (double)(pin >> 4 & 0x3) * 0.25 + 0.125, (double)(pin >> 8 & 0x3) * 0.25 + 0.125, face, o);
		int x = (orient & 3) - 2, y = (orient >> 2 & 3) - 2, z = (orient >> 4 & 3) - 2;
		Vec3d vec = o.rotate(new Vec3d(x, y, z));
		x = (int)vec.x; y = (int)vec.y; z = (int)vec.z;
		int l = x*x + y*y + z*z;
		int or;
		switch(l) {
		case 1:
			or = z != 0 ? (z < 0 ? 0 : 2) : x != 0 ? (x < 0 ? 3 : 1) : (y < 0 ? 4 : 12);
			break;
		case 2:
			if (y != 0) {
				or = x < 0 ? 4 : z < 0 ? 5 : z == 0 ? 6 : 7;
				if (y > 0) or += 8;
				break;
			}
		case 3:
			or = x > 0 ? 1 : 0;
			if (z > 0) or = 3 - or;
			if (y > 0) or = or + 1 & 3 | 8;
			break;
		default: or = 0;
		}
		orient = orient & 0x3f | or << 8 | l << 12;
	}

	@Override
	public <T> void addRenderComps(List<T> list, Class<T> type) {
		super.addRenderComps(list, type);
		if (type.isInstance(opposite.connector)) {
			list.add(type.cast(opposite.connector));
			opposite.connector.setPort(opposite);
		}
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (hit || player.isSneaking() && stack.isEmpty()) {
			if (connector != null) setConnector(null, player);
			else if (opposite.connector != null) opposite.setConnector(null, player);
			else ((IHookAttachable)owner).removeHook(pin, player);
			return true;
		}
		if (stack.getItem() instanceof IWiredConnectorItem) {
			((IWiredConnectorItem)stack.getItem()).doAttach(stack, connector != null && opposite.connector == null ? opposite : this, player);
			return true;
		} 
		return false;
	}

	@Override
	public void connect(SignalPort to) {
		if (!(to instanceof MountedSignalPort)) return;
		MountedSignalPort port = (MountedSignalPort)to;
		SignalLine line;
		try {line = new SignalLine(this);}
		catch (WireLoopException e) {return;}
		if (line.source == null || line.sink == null || !line.contains(port)) return;
		
		IConnector con = line.source.getConnector();
		String label = con instanceof ITagableConnector ? ((ITagableConnector)con).getTag() : null;
		if (label == null && (con = line.sink.getConnector()) instanceof ITagableConnector)
			label = ((ITagableConnector)con).getTag();
		String label_ = label;
		line.forEach((c)-> {
			IConnector cn = c.getConnector();
			if (cn instanceof ITagableConnector)
				((ITagableConnector)cn).setTag(c, label_);
		});
		
		line.source.connect(line.sink);
		int id = line.source.getLink();
		for (RelayPort rp : line.hooks) {
			rp.linkID = id;
			rp.owner.onPortModified(rp, ISignalIO.E_CONNECT);
		}
	}

	@Override
	public void disconnect() {
		SignalLine line;
		try {line = new SignalLine(this);}
		catch (WireLoopException e) {return;}
		if (line.source != null) line.source.disconnect();
		else if (line.sink != null) line.sink.disconnect();
		for (RelayPort rp : line.hooks) {
			rp.linkID = 0;
			rp.owner.onPortModified(rp, ISignalIO.E_DISCONNECT);
		}
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setByte("o", (byte)orient);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		orient = nbt.getByte("o");
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
