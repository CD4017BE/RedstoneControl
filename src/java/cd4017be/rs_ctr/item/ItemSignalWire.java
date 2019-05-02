package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import cd4017be.rs_ctr.api.wire.IWiredConnector.IWiredConnectorItem;
import cd4017be.rs_ctr.signal.WireConnection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * @author CD4017BE
 *
 */
public class ItemSignalWire extends BaseItem implements IWiredConnectorItem {

	public static int MAX_LENGTH = 16;

	/**
	 * @param id
	 */
	public ItemSignalWire(String id) {
		super(id);
	}

	@Override
	public void doAttach(ItemStack stack, MountedSignalPort port, EntityPlayer player) {
		boolean creative = player.isCreative();
		NBTTagCompound nbt = stack.getTagCompound();
		BlockPos pos = port.getPos();
		if (nbt == null) {
			stack.setTagCompound(nbt = new NBTTagCompound());
			nbt.setInteger("lx", pos.getX());
			nbt.setInteger("ly", pos.getY());
			nbt.setInteger("lz", pos.getZ());
			nbt.setInteger("lp", port.pin);
			nbt.setBoolean("d", port.isSource);
			return;
		}
		if (!nbt.getBoolean("d") ^ port.isSource) {
			player.sendMessage(new TextComponentString(TooltipUtil.translate("msg.rs_ctr.wire0")));
			stack.setTagCompound(null);
			return;
		}
		int lx = nbt.getInteger("lx"), ly = nbt.getInteger("ly"), lz = nbt.getInteger("lz");
		int d = (int)Math.ceil(pos.getDistance(lx, ly, lz));
		if (d > MAX_LENGTH || (!creative && d > stack.getCount())) {
			player.sendMessage(new TextComponentString(d > MAX_LENGTH ?
					TooltipUtil.format("msg.rs_ctr.wire2", MAX_LENGTH) :
						TooltipUtil.translate("msg.rs_ctr.wire1")));
			stack.setTagCompound(null);
			return;
		}
		if (creative) d = 0;
		BlockPos lpos = new BlockPos(lx, ly, lz);
		int lp = nbt.getInteger("lp");
		SignalPort p = ISignalIO.getPort(player.world, lpos, lp);
		if (!(p instanceof MountedSignalPort)) {
			player.sendMessage(new TextComponentString(TooltipUtil.translate("msg.rs_ctr.wire3")));
			stack.setTagCompound(null);
			return;
		}
		MountedSignalPort lport = (MountedSignalPort)p;
		//TODO no plugs on anchors
		Vec3d path = new Vec3d(lpos.subtract(pos))
				.add(lport.pos.subtract(port.pos))
				.add(new Vec3d(lport.face.getDirectionVec()).subtract(new Vec3d(port.face.getDirectionVec())).scale(0.125));
		port.setConnector(new WireConnection(lpos, lp, path.scale(0.5), d/2), player);
		lport.setConnector(new WireConnection(pos, port.pin, path.scale(-0.5), d - d/2), player);
		port.connect(lport);
		stack.setTagCompound(null);
		stack.shrink(d);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand hand) {
		if (player.isSneaking()) {
			ItemStack stack = player.getHeldItem(hand);
			stack.setTagCompound(null);
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}
		return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if (!isSelected) return;
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) return;
		Vec3d start = entity.getPositionEyes(1);
		RayTraceResult rt = world.rayTraceBlocks(start, start.add(entity.getLook(1).scale(3.0)));
		BlockPos pos = rt == null ? entity.getPosition() : rt.getBlockPos();
		nbt.setInteger("n", (int)Math.ceil(pos.getDistance(nbt.getInteger("lx"), nbt.getInteger("ly"), nbt.getInteger("lz"))));
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
