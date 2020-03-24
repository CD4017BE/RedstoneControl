package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.wire.WiredConnector;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class WireBranch extends WiredConnector implements ITESRenderComp, IBlockRenderComp {

	public final WireType type;
	protected Vec3d line;
	public int length;
	private float[] vertices; // render cache
	private int light1 = -1;

	public WireBranch(MountedPort port, WireType type) {
		super(port);
		this.type = type;
	}

	@Override
	protected String id() {
		return type.wiredId;
	}

	@Override
	public NBTTagCompound serializeNBT() {//TODO backwards compatibility
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setFloat("dx", (float)line.x);
		nbt.setFloat("dy", (float)line.y);
		nbt.setFloat("dz", (float)line.z);
		nbt.setByte("len", (byte)length);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		line = new Vec3d(nbt.getFloat("dx"), nbt.getFloat("dy"), nbt.getFloat("dz"));
		length = nbt.getByte("len") & 0xff;
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		super.onRemoved(player);
		dropItem(new ItemStack(type.wireItem, length), player);
	}

	@Override
	protected void onMoved(WiredConnector link) {
		int d = (int)Math.ceil(getDistance(port, link.port));
		if (d > length + (link instanceof WireBranch ? ((WireBranch)link).length : 0))
			onWireRemoved(link, null);
		else {
			line = getPath(port, link.port).scale(.5);
			port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if(line == null) return;
		if(vertices == null) vertices = WireRenderer.createLine(port, line);
		if(light1 < 0) light1 = port.getWorld().getCombinedLight(
			port.getPos().add(line.x + port.pos.x, line.y + port.pos.y, line.z + port.pos.z), 0
		);
		WireRenderer.drawLine(buffer, vertices, (float)x, (float)y, (float)z, light, light1, type.color);
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		if(line == null) return null;
		Vec3d p = new Vec3d(pos).add(port.pos);
		return new AxisAlignedBB(p, p.add(line));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		this.light1 = -1;
	}

	@Override
	public boolean isCompatible(Class<?> type) {
		return type == this.type.clazz;
	}
	
}
