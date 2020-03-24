package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IIntegratedConnector;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.wire.RelayPort;
import cd4017be.api.rs_ctr.wire.WiredConnector.IWiredConnectorItem;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.port.WireBranch;
import cd4017be.rs_ctr.port.WireConnection;
import cd4017be.rs_ctr.port.WireType;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * @author CD4017BE
 *
 */
public class ItemWireCon extends BaseItem implements IWiredConnectorItem {

	public static int MAX_LENGTH = 16;

	public final WireType type;

	public ItemWireCon(String id, WireType type) {
		super(id);
		this.type = type;
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		if (port.type != type.clazz && port.type != null) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		}
		boolean creative = player.isCreative();
		NBTTagCompound nbt = stack.getTagCompound();
		BlockPos pos = port.getPos();
		if (nbt == null) {
			stack.setTagCompound(nbt = new NBTTagCompound());
			nbt.setInteger("lx", pos.getX());
			nbt.setInteger("ly", pos.getY());
			nbt.setInteger("lz", pos.getZ());
			nbt.setInteger("lp", port.pin);
			nbt.setBoolean("d", port.isMaster);
			return;
		}
		stack.setTagCompound(null);
		if (!nbt.getBoolean("d") ^ port.isMaster) {
			if (port instanceof RelayPort) port = ((RelayPort)port).opposite;
			else {
				player.sendMessage(new TextComponentString(TooltipUtil.translate("msg.rs_ctr.wire0")));
				return;
			}
		}
		int lx = nbt.getInteger("lx"), ly = nbt.getInteger("ly"), lz = nbt.getInteger("lz");
		int d = (int)Math.ceil(pos.getDistance(lx, ly, lz));
		if (d > MAX_LENGTH || (!creative && d > stack.getCount())) {
			player.sendMessage(new TextComponentString(d > MAX_LENGTH ?
					TooltipUtil.format("msg.rs_ctr.wire2", MAX_LENGTH) :
						TooltipUtil.translate("msg.rs_ctr.wire1")));
			return;
		}
		BlockPos lpos = new BlockPos(lx, ly, lz);
		Port p = IPortProvider.getPort(player.world, lpos, nbt.getInteger("lp"));
		if (!(p instanceof MountedPort)) {
			player.sendMessage(new TextComponentString(TooltipUtil.translate("msg.rs_ctr.wire3")));
			return;
		}
		MountedPort lport = (MountedPort)p;
		WireBranch conA, conB;
		Connector con;
		if ((con = port.getConnector()) instanceof IIntegratedConnector) {
			if (!((IIntegratedConnector)con).addWire(conA = new WireBranch(port, type), player, true))
				return;
		} else conA = new WireConnection(port, type);
		if ((con = lport.getConnector()) instanceof IIntegratedConnector) {
			if (!((IIntegratedConnector)con).addWire(conB = new WireBranch(lport, type), player, false))
				return;
		} else lport.setConnector(conB = new WireConnection(lport, type), player);
		if ((con = port.getConnector()) instanceof IIntegratedConnector)
			((IIntegratedConnector)con).addWire(conA, player, false);
		else port.setConnector(conA, player);
		conA.length = d / 2;
		conB.length = d - conA.length;
		if (!player.isCreative()) stack.shrink(d);
		conA.connect(conB);
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
