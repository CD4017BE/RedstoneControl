package cd4017be.rs_ctr.signal;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WireConnection implements IConnector {

	public static final String ID = "wire";

	private BlockPos linkPos;
	private int linkPin;
	private Vec3d line;
	private int count;

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
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		count = nbt.getByte("count") & 0xff;
		line = new Vec3d(nbt.getFloat("dx"), nbt.getFloat("dy"), nbt.getFloat("dz"));
		linkPos = BlockPos.fromLong(nbt.getLong("pos"));
		linkPin = nbt.getInteger("pin");
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
	public void renderConnection(World world, BlockPos pos, MountedSignalPort port, double x, double y, double z, BufferBuilder b) {
		if (vertices == null) vertices = WireRenderer.instance.createLine(port, line);
		int l0 = world.getCombinedLight(pos, 0), l1 = world.getCombinedLight(pos.add(line.x + port.pos.x, line.y + port.pos.y, line.z + port.pos.z), 0);
		WireRenderer.instance.drawPlug(b, port, (float)x, (float)y, (float)z, l0, 0);
		WireRenderer.instance.drawLine(b, vertices, (float)x, (float)y, (float)z, l0, l1, 0xffffffff);
	}

	@Override
	public AxisAlignedBB renderSize(World world, BlockPos pos, MountedSignalPort port) {
		Vec3d p = new Vec3d(pos).add(port.pos);
		return new AxisAlignedBB(p, p.add(line));
	}

}