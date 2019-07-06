package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.IConnector.IConnectorItem;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.port.BlockProbe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;


/**
 * @author CD4017BE
 *
 */
public class ItemBlockProbe extends BaseItem implements IConnectorItem {

	public static int MAX_LENGTH = 8;

	/**
	 * @param id
	 */
	public ItemBlockProbe(String id) {
		super(id);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!player.isSneaking()) return EnumActionResult.PASS;
		ItemStack stack = player.getHeldItem(hand);
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		nbt.setInteger("lx", pos.getX());
		nbt.setInteger("ly", pos.getY());
		nbt.setInteger("lz", pos.getZ());
		nbt.setByte("lf", (byte)facing.getIndex());
		return EnumActionResult.SUCCESS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (!player.isSneaking()) return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
		stack.setTagCompound(null);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (port.type != BlockHandler.class) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		} else if (nbt == null) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.probe0"));
			return;
		} else if (port.isMaster) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.probe1"));
			return;
		}
		boolean creative = player.isCreative();
		EnumFacing side = EnumFacing.getFront(nbt.getByte("lf"));
		BlockPos pos = new BlockPos(nbt.getInteger("lx"), nbt.getInteger("ly"), nbt.getInteger("lz"));
		int d = (int)Math.ceil(Math.sqrt(pos.offset(side).distanceSq(port.getPos())));
		if (d > MAX_LENGTH || (!creative && d > stack.getCount())) {
			player.sendMessage(new TextComponentString(d > MAX_LENGTH ?
					TooltipUtil.format("msg.rs_ctr.wire2", MAX_LENGTH) :
						TooltipUtil.translate("msg.rs_ctr.wire1")));
			stack.setTagCompound(null);
			return;
		}
		if (creative) d = 0;
		port.setConnector(new BlockProbe(pos, side, d), player);
		stack.setTagCompound(null);
		stack.shrink(d);
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if (!isSelected) return;
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) return;
		Vec3d start = entity.getPositionEyes(1);
		RayTraceResult rt = world.rayTraceBlocks(start, start.add(entity.getLook(1).scale(3.0)));
		BlockPos pos = rt == null ? entity.getPosition() : rt.getBlockPos();
		EnumFacing side = EnumFacing.getFront(nbt.getByte("lf"));
		nbt.setInteger("n", (int)Math.ceil(pos.getDistance(nbt.getInteger("lx") + side.getFrontOffsetX(), nbt.getInteger("ly") + side.getFrontOffsetY(), nbt.getInteger("lz") + side.getFrontOffsetZ())));
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return stack.getTagCompound() != null;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		return (double)nbt.getInteger("n") / (double)Math.min(stack.getCount(), MAX_LENGTH);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return !oldStack.isItemEqual(newStack);
	}

}
