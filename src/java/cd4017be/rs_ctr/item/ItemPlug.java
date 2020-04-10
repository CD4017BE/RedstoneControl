package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IIntegratedConnector;
import cd4017be.api.rs_ctr.port.Connector.IConnectorItem;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.item.BaseItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

/** @author CD4017BE */
public abstract class ItemPlug extends BaseItem implements IConnectorItem {

	protected final Class<?> type;
	protected final boolean master;

	public ItemPlug(String id, Class<?> type, boolean master) {
		super(id);
		this.type = type;
		this.master = master;
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		if(port.type != type) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		} else if(port.isMaster ^ master) {
			player.sendMessage(new TextComponentTranslation(master ? "msg.rs_ctr.dir_out" : "msg.rs_ctr.dir_in"));
			return;
		}
		Connector con = create(port, stack, player);
		if(con == null) return;
		port.setConnector(con, player);
		if (con instanceof IIntegratedConnector)
			((IIntegratedConnector)con).getPort(0).connect(port);
		if(!player.isCreative()) stack.shrink(1);
	}

	protected abstract Connector create(MountedPort port, ItemStack stack, EntityPlayer player);

}
