package cd4017be.rs_ctr.port;

import java.util.List;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** @author CD4017BE */
public class EdgeTrigger extends LogicPlug {

	public static final String ID = "edge_trigger";

	int state;
	public boolean rising;

	public EdgeTrigger(MountedPort port) {
		super(port);
	}

	@Override
	public void updateSignal(int value) {
		if (value <= 0 ^ (state & 0x10000) != 0) return;
		state ^= 0x10000;
		if (value <= 0 ^ rising)
			out.updateSignal((state ^= 0xffff) & 0xffff);
	}

	@Override
	protected int getOutput() {
		return state & 0xffff;
	}

	@Override
	protected String id() {
		return ID;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("state", state);
		nbt.setBoolean("mode", rising);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		state = nbt.getInteger("state");
		rising = nbt.getBoolean("mode");
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		super.onRemoved(player);
		dropItem(new ItemStack(Objects.edge_trigger, 1, rising ? 0 : 1), player);
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		return TooltipUtil.translate(rising ? "port.rs_ctr.edge0" : "port.rs_ctr.edge1") + super.displayInfo(port, outPort.getLink());
	}

	@Override
	public void render(List<BakedQuad> quads) {
		super.render(quads);
		PortRenderer.PORT_RENDER.drawModel(
			quads, (float)port.pos.x, (float)port.pos.y, (float)port.pos.z,
			Orientation.fromFacing(port.face), "_plug.misc(4)"
		);
	}

}
