package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItem;
import cd4017be.rs_ctr.tileentity.part.Module;
import cd4017be.rs_ctr.tileentity.part.Module.IPanel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 
 * @author cd4017be
 */
public class ItemPanelModule extends BaseItem {

	final String module;

	public ItemPanelModule(String id, String module) {
		super(id);
		this.module = module;
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof IPanel)) return EnumActionResult.PASS;
		Module m = Module.get(module);
		if (m == null) return EnumActionResult.FAIL;
		if (world.isRemote) return EnumActionResult.SUCCESS;
		IPanel panel = (IPanel)te;
		Vec3d hit = panel.getOrientation().invRotate(new Vec3d(hitX - .5F, hitY - .5F, hitZ - .5F));
		ItemStack stack = player.getHeldItem(hand);
		m.onPlaced(stack, (float)(hit.x + .5), (float) (hit.y + .5));
		if (!panel.add(m)) return EnumActionResult.FAIL;
		if (!player.isCreative()) stack.shrink(1);
		return EnumActionResult.SUCCESS;
	}

}
