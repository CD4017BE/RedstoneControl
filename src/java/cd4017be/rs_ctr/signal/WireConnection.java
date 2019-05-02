package cd4017be.rs_ctr.signal;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.ITagableConnector;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import cd4017be.rs_ctr.api.wire.IWiredConnector;
import cd4017be.rs_ctr.api.wire.SignalLine;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
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
public class WireConnection implements ITagableConnector, IWiredConnector {

	public static final String ID = "wire";

	private BlockPos linkPos;
	private int linkPin;
	private Vec3d line;
	private int count;
	private String tag;

	public WireConnection() {}

	public WireConnection(BlockPos linkPos, int linkPin, Vec3d line, int count) {
		this.linkPos = linkPos;
		this.linkPin = linkPin;
		this.line = line;
		this.count = count;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("id", ID);
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
		ItemStack stack = new ItemStack(Objects.wire, count);
		World world = port.getWorld();
		BlockPos pos = port.getPos();
		if (player != null) ItemFluidUtil.dropStack(stack, player);
		else ItemFluidUtil.dropStack(stack, world, pos);
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

	private float[] vertices; //render cache

	@Override
	@SideOnly(Side.CLIENT)
	public void renderConnection(World world, BlockPos pos, MountedSignalPort port, double x, double y, double z, int light, BufferBuilder b) {
		//TODO no plug for anchor
		if (vertices == null) vertices = WireRenderer.instance.createLine(port, line);
		WireRenderer.instance.drawModel(b, (float)x, (float)y, (float)z, Orientation.fromFacing(port.face), light, "plug.main(0)");
		int l1 = world.getCombinedLight(pos.add(line.x + port.pos.x, line.y + port.pos.y, line.z + port.pos.z), 0);
		WireRenderer.instance.drawLine(b, vertices, (float)x, (float)y, (float)z, light, l1, 0xffffffff);
	}

	@Override
	public AxisAlignedBB renderSize(World world, BlockPos pos, MountedSignalPort port) {
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

}