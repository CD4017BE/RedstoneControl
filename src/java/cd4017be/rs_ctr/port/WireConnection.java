package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.wire.RelayPort;
import cd4017be.lib.util.DimPos;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class WireConnection extends WireBranch implements ITagableConnector, IBlockRenderComp {

	public WireConnection(MountedPort port, WireType type) {
		super(port, type);
	}

	public WireConnection(MountedPort port, BlockPos linkPos, int linkPin, Vec3d line, int count, WireType type) {
		super(port, type);
		this.conPos = new DimPos(linkPos, port.getWorld());
		this.conPin = linkPin;
		this.line = line;
		this.length = count;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		super.render(quads);
		if(port instanceof RelayPort) return;
		PortRenderer.PORT_RENDER.drawModel(
			quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z,
			Orientation.fromFacing(port.face), type.wireModel()
		);
	}

}
