package cd4017be.rs_ctr.item;

import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.DimPos;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.signal.IConnector.IConnectorItem;
import cd4017be.rs_ctr.signal.WireType;
import cd4017be.rs_ctr.signal.WirelessConnection;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;


/**
 * @author CD4017BE
 *
 */
public class ItemWirelessCon extends BaseItem implements IConnectorItem {

	public final WireType type;

	public ItemWirelessCon(String id, WireType type) {
		super(id);
		this.type = type;
	}

	@Override
	public void doAttach(ItemStack stack, MountedSignalPort port, EntityPlayer player) {
		if (port.type != type.clazz) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		}
		NBTTagCompound nbt = stack.getTagCompound();
		DimPos pos = new DimPos(port.getPos(), port.getWorld());
		if (nbt == null) {
			stack.setTagCompound(nbt = new NBTTagCompound());
			nbt.setInteger("lx", pos.getX());
			nbt.setInteger("ly", pos.getY());
			nbt.setInteger("lz", pos.getZ());
			nbt.setInteger("ld", pos.dimId);
			nbt.setInteger("lp", port.pin);
			nbt.setBoolean("d", port.isMaster);
			return;
		}
		if (!nbt.getBoolean("d") ^ port.isMaster) {
			player.sendMessage(new TextComponentString(TooltipUtil.translate("msg.rs_ctr.wire0")));
			return;
		}
		DimPos lpos = new DimPos(nbt.getInteger("lx"), nbt.getInteger("ly"), nbt.getInteger("lz"), nbt.getInteger("ld"));
		int lp = nbt.getInteger("lp");
		SignalPort p = ISignalIO.getPort(lpos.getWorldServer(), lpos, lp);
		if (!(p instanceof MountedSignalPort)) {
			player.sendMessage(new TextComponentString(TooltipUtil.translate("msg.rs_ctr.wire3")));
			stack.setTagCompound(null);
			return;
		}
		MountedSignalPort lport = (MountedSignalPort)p;
		
		port.setConnector(new WirelessConnection(lpos, lp, !player.isCreative(), type), player);
		lport.setConnector(new WirelessConnection(pos, port.pin, false, type), player);
		port.connect(lport);
		stack.setTagCompound(null);
		if (!player.isCreative()) stack.shrink(1);
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
	public boolean showDurabilityBar(ItemStack stack) {
		return stack.hasTagCompound();
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return 0;
	}

}
