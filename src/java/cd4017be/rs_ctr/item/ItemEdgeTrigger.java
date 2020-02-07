package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.IConnector.IConnectorItem;
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
	protected IConnector create(ItemStack stack, EntityPlayer player) {
		return new EdgeTrigger(stack.getMetadata() == 0);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand handIn) {
		ItemStack stack = player.getHeldItem(handIn).copy();
		stack.setItemDamage(stack.getMetadata() ^ 1);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

}
