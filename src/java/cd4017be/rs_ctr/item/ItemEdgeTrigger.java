package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.Connector.IConnectorItem;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.rs_ctr.port.EdgeTrigger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/** @author CD4017BE */
public class ItemEdgeTrigger extends ItemPlug implements IConnectorItem {

	public ItemEdgeTrigger(String id) {
		super(id, SignalHandler.class, true);
		setHasSubtypes(true);
	}

	@Override
	protected Connector create(MountedPort port, ItemStack stack, EntityPlayer player) {
		EdgeTrigger con = new EdgeTrigger(port);
		con.rising = stack.getMetadata() == 0;
		return con;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand handIn) {
		ItemStack stack = player.getHeldItem(handIn).copy();
		stack.setItemDamage(stack.getMetadata() ^ 1);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

}
