package cd4017be.rs_ctr.port;

import java.util.List;

import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.wire.RelayPort;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class WireAnchor extends RelayPort implements IBlockRenderComp {

	/** the anchor direction for render: 0bZZYYXX, X/Y/Z = {0b01, 0b10, 0b11} */
	public int orient;

	public WireAnchor(IPortProvider owner, int pin) {
		super(owner, pin);
		this.orient = pin >> 16 & 0x3f;
	}

	public WireAnchor(WireAnchor opposite) {
		super(opposite);
	}

	@Override
	protected RelayPort createPair() {
		return new WireAnchor(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		PortRenderer.PORT_RENDER.drawModel(quads, (float)pos.x, (float)pos.y, (float)pos.z, Orientation.values()[orient >> 8 & 15], "_hook.pin(" + Integer.toString(orient >> 12 & 3) + ")");
	}

	@Override
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
	public ItemStack getDropped() {
		return new ItemStack(Objects.wire_anchor);
	}

}
