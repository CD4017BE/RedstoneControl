package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.wire.RelayPort;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class WireConnection extends WireBranch implements ITagableConnector, IBlockRenderComp {

	private String tag;

	public WireConnection(WireType type) {
		super(type);
	}

	public WireConnection(BlockPos linkPos, int linkPin, Vec3d line, int count, WireType type) {
		super(type);
		this.linkPos = linkPos;
		this.linkPin = linkPin;
		this.line = line;
		this.count = count;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		if(tag != null) nbt.setString("tag", tag);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		tag = nbt.hasKey("tag", NBT.TAG_STRING) ? nbt.getString("tag") : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		super.render(quads);
		if(port instanceof RelayPort) return;
		PortRenderer.PORT_RENDER.drawModel(
			quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z,
			Orientation.fromFacing(port.face), type.model
		);
	}

	@Override
	public void setTag(MountedPort port, String tag) {
		this.tag = tag;
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
	}

	@Override
	public String getTag() {
		return tag;
	}

}
