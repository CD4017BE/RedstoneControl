package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/** 
 * @author CD4017BE */
public class RemoteReceiver extends Connector implements IBlockRenderComp {

	public static final String ID = "remote";

	public RemoteReceiver(MountedPort port) {
		super(port);
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		port.disconnect();
	}

	@Override
	protected String id() {
		return ID;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(
			quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z,
			Orientation.fromFacing(port.face), "_plug.wireless(0)"
		);
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		return "\n" + TooltipUtil.format("port.rs_ctr.remote", linkID);
	}

}
