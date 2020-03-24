package cd4017be.rs_ctr.item;

import cd4017be.api.rs_ctr.port.Connector.IConnectorItem;
import cd4017be.api.rs_ctr.port.IPortProvider;
import java.util.function.Function;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.item.BaseItem;
import cd4017be.rs_ctr.port.SplitPlug;
import cd4017be.rs_ctr.port.WireType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

/** @author CD4017BE */
public class ItemSplitCon extends BaseItem implements IConnectorItem {

	public final WireType type;
	public final Function<MountedPort, SplitPlug> constructor;

	public ItemSplitCon(String id, WireType type, Function<MountedPort, SplitPlug> constructor) {
		super(id);
		this.type = type;
		this.constructor = constructor;
		Connector.REGISTRY.put(type.splitId, constructor);
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		if(port.type != type.clazz) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		} else if(!port.isMaster) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.dir_out"));
			return;
		}
		int n;
		Connector con = port.getConnector();
		if(con instanceof SplitPlug) {
			SplitPlug sp = (SplitPlug)con;
			n = sp.addLinks(1);
			if(n > 0) port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
		} else {
			SplitPlug sp = constructor.apply(port);
			if (player.isCreative() || (n = stack.getCount()) > 2) n = 2;
			n = sp.addLinks(n);
			port.setConnector(sp, player);
			port.connect(sp.inPort);
		}
		if(!player.isCreative()) stack.shrink(n);
	}

}
