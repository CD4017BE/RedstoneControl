package cd4017be.rs_ctr.signal;

import java.util.List;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.ITagableConnector;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import cd4017be.rs_ctr.api.wire.IWiredConnector;
import cd4017be.rs_ctr.api.wire.RelayPort;
import cd4017be.rs_ctr.render.PortRenderer;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
public class WireConnection extends Plug implements ITagableConnector, IWiredConnector, IBlockRenderComp, ITESRenderComp {

	public static final String ID = "wire";

	private final WireType type;
	private BlockPos linkPos;
	private int linkPin;
	private Vec3d line;
	private int count;
	private String tag;

	public WireConnection(WireType type) {
		this.type = type;
	}

	public WireConnection(BlockPos linkPos, int linkPin, Vec3d line, int count, WireType type) {
		this(type);
		this.linkPos = linkPos;
		this.linkPin = linkPin;
		this.line = line;
		this.count = count;
	}

	@Override
	protected String id() {
		return type.wiredId;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setLong("pos", linkPos.toLong());
		nbt.setInteger("pin", linkPin);
		nbt.setFloat("dx", (float)line.x);
		nbt.setFloat("dy", (float)line.y);
		nbt.setFloat("dz", (float)line.z);
		nbt.setByte("count", (byte)count);
		if (tag != null) nbt.setString("tag", tag);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		count = nbt.getByte("count") & 0xff;
		line = new Vec3d(nbt.getFloat("dx"), nbt.getFloat("dy"), nbt.getFloat("dz"));
		linkPos = BlockPos.fromLong(nbt.getLong("pos"));
		linkPin = nbt.getInteger("pin");
		tag = nbt.hasKey("tag", NBT.TAG_STRING) ? nbt.getString("tag") : null;
	}

	@Override
	public void onRemoved(MountedSignalPort port, EntityPlayer player) {
		super.onRemoved(port, player);
		World world = port.getWorld();
		BlockPos pos = port.getPos();
		SignalPort p = ISignalIO.getPort(world, linkPos, linkPin);
		if (p instanceof MountedSignalPort) {
			IConnector c = ((MountedSignalPort)p).getConnector();
			if (c instanceof WireConnection) {
				WireConnection wc = (WireConnection)c;
				if (wc.linkPos.equals(pos) && wc.linkPin == port.pin)
					((MountedSignalPort)p).setConnector(null, player);
			}
		}
		port.disconnect();
	}

	@Override
	protected ItemStack drop() {
		return new ItemStack(Objects.wire, count);
	}

	private float[] vertices; //render cache
	private int light1 = -1;

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (vertices == null) vertices = WireRenderer.createLine(port, line);
		if (light1 < 0) light1 = port.getWorld().getCombinedLight(port.getPos().add(line.x + port.pos.x, line.y + port.pos.y, line.z + port.pos.z), 0);
		WireRenderer.drawLine(buffer, vertices, (float)x, (float)y, (float)z, light, light1, type == WireType.SIGNAL ? 0xff0000ff : 0xff00ffff);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		this.light1 = -1;
		if (port instanceof RelayPort) return;
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), "_plug.main(" + (type == WireType.SIGNAL ? 0 : 7) + ")");
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		Vec3d p = new Vec3d(pos).add(port.pos);
		return new AxisAlignedBB(p, p.add(line));
	}

	@Override
	public void setTag(MountedSignalPort port, String tag) {
		this.tag = tag;
		port.owner.onPortModified(port, ISignalIO.E_CON_UPDATE);
	}

	@Override
	public String getTag() {
		return tag;
	}

	@Override
	public BlockPos getLinkPos() {
		return linkPos;
	}

	@Override
	public int getLinkPin() {
		return linkPin;
	}

	@Override
	public boolean isCompatible(Class<?> type) {
		return type == this.type.clazz;
	}

}