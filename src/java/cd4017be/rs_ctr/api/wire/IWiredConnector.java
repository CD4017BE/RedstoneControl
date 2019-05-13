package cd4017be.rs_ctr.api.wire;

import java.util.List;

import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A connector that can be attached to wire anchors and connects between blocks in the same world.
 * @see IConnector
 * @author cd4017be
 */
public interface IWiredConnector extends IConnector {

	/**
	 * @return the block location of the linked port.
	 */
	BlockPos getLinkPos();

	/**
	 * @return the pin id of the linked port.
	 */
	int getLinkPin();

	/**
	 * render only the wire
	 * @param quads see {@link IBlockRenderComp#render(List)}
	 * @param anchor anchor point of this wired connector
	 */
	@SideOnly(Side.CLIENT)
	void renderWire(List<BakedQuad> quads, Vec3d anchor);

	/**
	 * implemented by {@link Item}s that want to interact with {@link RelayPort}s.
	 * @see IConnectorItem
	 * @author cd4017be
	 */
	public interface IWiredConnectorItem extends IConnectorItem {

		/**
		 * Perform attachment of given connector item on given RelayPort by calling {@link MountedSignalPort#setConnector(IConnector, EntityPlayer)} and eventually {@link RelayPort#connect(SignalPort)}.
		 * @param stack the itemstack used
		 * @param port the port to interact with
		 * @param player the interacting player
		 */
		default void doAttach(ItemStack stack, RelayPort port, EntityPlayer player) {
			doAttach(stack, (MountedSignalPort)port, player);
		}

	}

}
