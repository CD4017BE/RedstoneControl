package cd4017be.rs_ctr.port;

import java.util.List;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.render.PortRenderer;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class BlockProbe extends Connector implements IBlockRenderComp, ITESRenderComp {

	public static final String ID = "blockProbe";

	private BlockPos linkPos;
	private EnumFacing linkFace;

	public BlockProbe(MountedPort port, BlockPos linkPos, EnumFacing linkFace) {
		super(port);
		this.linkPos = linkPos.subtract(port.getPos());
		this.linkFace = linkFace;
	}

	public BlockProbe(MountedPort port) {
		super(port);
	}

	@Override
	protected String id() {
		return ID;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setLong("pos", linkPos.toLong());
		nbt.setByte("side", (byte)linkFace.getIndex());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		linkPos = BlockPos.fromLong(nbt.getLong("pos"));
		linkFace = EnumFacing.getFront(nbt.getByte("side"));
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		port.owner.onPortModified(port, IPortProvider.E_DISCONNECT);
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("lx", linkPos.getX());
		nbt.setInteger("ly", linkPos.getY());
		nbt.setInteger("lz", linkPos.getZ());
		nbt.setByte("lf", (byte)linkFace.getIndex());
		ItemStack stack = new ItemStack(Objects.block_wire);
		stack.setTagCompound(nbt);
		dropItem(stack, player);
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		String name = port.getWorld().getBlockState(port.getPos().add(linkPos)).getBlock().getLocalizedName();
		if (name.endsWith(".name")) name = name.substring(0, name.length() - 5);
		return "\n\u00a7b" + name + "\u00a7f " + TooltipUtil.translate("enumfacing." + linkFace.name().toLowerCase());
	}

	@Override
	public void onLoad() {
		((BlockHandler)port.owner.getPortCallback(port.pin)).updateBlock(new BlockReference(port.getWorld(), port.getPos().add(linkPos), linkFace));
	}

	private float[] vertices; //render cache
	private int light1 = -1;

	@Override
	@SideOnly(Side.CLIENT)
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if (vertices == null) {
			double dx = (double)linkFace.getFrontOffsetX() * .375 + (double)linkPos.getX() + .5 - (double)port.face.getFrontOffsetX() * .125,
					dy = (double)linkFace.getFrontOffsetY() * .375 + (double)linkPos.getY() + .5 - (double)port.face.getFrontOffsetY() * .125,
					dz = (double)linkFace.getFrontOffsetZ() * .375 + (double)linkPos.getZ() + .5 - (double)port.face.getFrontOffsetZ() * .125;
			vertices = WireRenderer.createLine(port, new Vec3d(dx, dy, dz).subtract(port.pos));
		}
		if (light1 < 0) light1 = port.getWorld().getCombinedLight(pos.add(linkPos).offset(linkFace), 0);
		WireRenderer.drawLine(buffer, vertices, (float)x, (float)y, (float)z, light, light1, 0xffffff00);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		this.light1 = -1;
		PortRenderer.PORT_RENDER.drawModel(quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z, Orientation.fromFacing(port.face), WireType.BLOCK.wireModel());
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return new AxisAlignedBB(new Vec3d(pos).add(port.pos), new Vec3d(new BlockPos(1,1,1).add(linkFace.getDirectionVec())).scale(0.5).add(new Vec3d(pos.add(linkPos))));
	}

}
