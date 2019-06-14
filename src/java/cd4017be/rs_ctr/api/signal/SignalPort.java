package cd4017be.rs_ctr.api.signal;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Represents the source or sink end point of a signal connection.<dl>
 * 
 * Each SignalPort instance must be uniquely identified by its owner and pin id.<br>
 * By convention SignalPorts hosted by TileEntities should use IDs >= 0 whereas ones hosted by Entities should use negative IDs.
 * And when multiple Entities are cramped into the same BlockPos they should use unique pin IDs for their Ports if possible so that additionally each port could be identified by a BlockPos and a pin ID.
 * @author CD4017BE
 */
public abstract class SignalPort implements INBTSerializable<NBTTagCompound> {

	/**the block, entity or whatever that is providing the port */
	public final ISignalIO owner;
	/**number to identify this port if the owner provides multiple ports */
	public final int pin;
	/**true if this is a source port, false if it is a sink port */
	public final boolean isMaster;
	/**the callback type class */
	public final Class<?> type;
	/**identifies the connection of this port in that two connected ports have the same linkID (all unconnected ports have linkID 0). */
	protected int linkID = 0;

	public int getLink() {
		return linkID;
	}

	/**
	 * @param owner the block, entity or whatever that is providing the port
	 * @param pin number to identify this port if the owner provides multiple ports
	 * @param type the type of interaction this port will use or provide
	 * @param isSource true if this is a source port, false if it is a sink port
	 */
	public SignalPort(ISignalIO owner, int pin, Class<?> type, boolean isSource) {
		this.owner = owner;
		this.pin = pin;
		this.type = type;
		this.isMaster = isSource;
	}

	/**
	 * make the port available to game interactions,
	 * so usually called when the host TileEntity/Entity got loaded.
	 */
	public void onLoad() {
		if (linkID == 0) return;
		Link l = Link.links.get(linkID);
		if (l == null) Link.links.put(linkID, new Link(this));
		else l.load(this);
	}

	/**
	 * make the port unavailable to game interactions.
	 * <b>Important: you must call this when the owner becomes unavailable (destroyed or chunk unload) to avoid memory leaks or stuff interacting with dead objects!</b>
	 */
	public void onUnload() {
		Link l = Link.links.get(linkID);
		if (l != null) l.unload(this);
	}

	/**
	 * connect this port to the given port
	 * @param to port to connect with (must be the opposite signal direction of this one).
	 */
	public void connect(SignalPort to) {
		if (linkID != 0) disconnect();
		if (to.linkID != 0) to.disconnect();
		if (isMaster) new Link(this, to);
		else new Link(to, this);
	}

	/**
	 * remove the connection between this port and whatever it is connected with
	 */
	public void disconnect() {
		Link l = Link.links.get(linkID);
		if (l != null) l.disconnect();
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("link", linkID);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		linkID = nbt.getInteger("link");
	}

	public BlockPos getPos() {
		return owner instanceof TileEntity ? ((TileEntity)owner).getPos() :
			owner instanceof Entity ? ((Entity)owner).getPosition() :
					null;
	}

	public World getWorld() {
		return owner instanceof TileEntity ? ((TileEntity)owner).getWorld() :
			owner instanceof Entity ? ((Entity)owner).world :
					null;
	}

}
