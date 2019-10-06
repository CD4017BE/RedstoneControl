package cd4017be.rs_ctr.tileentity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.network.IPlayerPacketReceiver;
import cd4017be.lib.network.IServerPacketReceiver;
import cd4017be.lib.network.SyncNetworkHandler;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.CommonProxy;
import cd4017be.rs_ctr.render.PortRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public class PowerHub extends WallMountGate implements EnergyHandler, IEnergyStorage, IUpdatable, IPlayerPacketReceiver, IServerPacketReceiver, IInteractiveComponent, IBlockRenderComp {

	public static long FE_UNIT = 100;

	ItemStack battery = ItemStack.EMPTY;
	public long charge, cap;

	{
		ports = new MountedPort[8];
		for (int i = 0; i < 8; i++)
			ports[i] = new MountedPort(this, i, EnergyHandler.class, false).setLocation(i < 4 ? 0.125 : 0.875, 0.125 + (i & 3) * 0.25, 0.5, EnumFacing.SOUTH).setName("port.rs_ctr.energy_io");
		//ports[8] = new MountedSignalPort(this, 8, SignalHandler.class, true).setName("port.rs_ctr.charge_o");
	}

	@Override
	public EnergyHandler getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
	}

	@Override
	protected void resetPin(int pin) {
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		nbt.setLong("E", charge);
		nbt.setLong("C", cap);
		if (!battery.isEmpty())
			nbt.setTag("bat", battery.writeToNBT(new NBTTagCompound()));
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		charge = nbt.getLong("E");
		cap = nbt.getLong("C");
		battery = nbt.hasKey("bat", NBT.TAG_COMPOUND) ? new ItemStack(nbt.getCompoundTag("bat")) : ItemStack.EMPTY;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityEnergy.ENERGY && facing == o.front;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == CapabilityEnergy.ENERGY && facing == o.front ? (T) this : null;
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(this);
	}

	@Override
	public void breakBlock() {
		super.breakBlock();
		onRemove(null);
		charge = 0;
		cap = 0;
	}

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		return IInteractiveComponent.rayTraceFlat(start, dir, new Vec3d(0.5, 0.5, 0.5), o.back, 0.25F, 0.375F);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (hit || player.isSneaking() && stack.isEmpty()) {
			onRemove(player);
			charge = 0;
			cap = 0;
			markDirty(REDRAW);
		} else if (!stack.isEmpty()) {
			IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY, null);
			if (es != null && es.canExtract() && es.canReceive()) {
				onRemove(player);
				charge += (long)es.extractEnergy(Integer.MAX_VALUE, false) * FE_UNIT;
				cap = es.receiveEnergy(Integer.MAX_VALUE, true) * FE_UNIT;
			} else if ((cap = CommonProxy.getCap(stack)) > 0) {
				onRemove(player);
			} else return false;
			battery = stack;
			player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
			if (charge > cap) charge = cap;
			markDirty(REDRAW);
		} else return false;
		return true;
	}

	private void onRemove(EntityPlayer player) {
		if (battery.isEmpty()) return;
		IEnergyStorage es = battery.getCapability(CapabilityEnergy.ENERGY, null);
		if (es != null) charge -= es.receiveEnergy((int)(charge / FE_UNIT), false) * FE_UNIT;
		if (player != null)
			ItemFluidUtil.dropStack(battery, player);
		else ItemFluidUtil.dropStack(battery, world, PowerHub.this.pos);
		this.battery = ItemStack.EMPTY;
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		return Pair.of(new Vec3d(0.5, 0.5, 0.5), battery.isEmpty() ? 
				TooltipUtil.translate("port.rs_ctr.battery0") :
				battery.getDisplayName() + "\n" + TooltipUtil.format("port.rs_ctr.battery", (double)charge / 1000D, (double)cap / 1000D));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		if (battery.isEmpty()) return;
		PortRenderer.PORT_RENDER.drawModel(quads, 0.5F, 0.5F, 0.5F, o, "_battery");
	}

	@Override
	public int changeEnergy(int dE, boolean sim) {
		long E = charge + dE;
		if (E < 0) {
			dE -= E;
			E = 0;
		} else if (E > cap) {
			dE -= E - cap;
			E = cap;
		}
		if (!sim) {
			charge = E;
			markDirty(SAVE);
		}
		return dE;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		long dE = (cap - charge) / FE_UNIT;
		if (dE < maxReceive) maxReceive = (int)dE;
		if (!simulate) {
			charge += (long)maxReceive * FE_UNIT;
			markDirty(SAVE);
		}
		return maxReceive;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		long dE = charge / FE_UNIT;
		if (dE < maxExtract) maxExtract = (int)dE;
		if (!simulate) {
			charge -= (long)maxExtract * FE_UNIT;
			markDirty(SAVE);
		}
		return maxExtract;
	}

	@Override
	public int getEnergyStored() {
		return (int)(charge / FE_UNIT);
	}

	@Override
	public int getMaxEnergyStored() {
		return (int)(cap / FE_UNIT);
	}

	@Override
	public boolean canExtract() {
		return cap > 0;
	}

	@Override
	public boolean canReceive() {
		return cap > 0;
	}

	//state synchronization
	Set<EntityPlayerMP> watching = null;
	boolean update;

	@Override
	protected void onUnload() {
		super.onUnload();
		watching = null;
	}

	@Override
	public void markDirty(int mode) {
		super.markDirty(mode);
		if (mode == SAVE && watching != null && !update) {
			update = true;
			TickRegistry.schedule(this);
		}
	}

	@Override
	public void process() {
		update = false;
		if (watching == null) return;
		for (Iterator<EntityPlayerMP> it = watching.iterator(); it.hasNext();) {
			EntityPlayerMP player = it.next();
			if (player.isDead || player.getDistanceSqToCenter(pos) > 30)
				it.remove(); //player is not looking at me anymore
		}
		if (watching.isEmpty()) watching = null;
		else {
			PacketBuffer pkt = SyncNetworkHandler.preparePacket(pos);
			pkt.writeLong(charge);
			SyncNetworkHandler.instance.sendToPlayers(pkt, watching);
		}
	}

	@Override
	public void handleServerPacket(PacketBuffer pkt) throws Exception {
		charge = pkt.readLong();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasFastRenderer() {
		if (super.hasFastRenderer()) {
			watching = null;
			return true;
		} else if (watching == null) {
			watching = Collections.emptySet();
			//hey server block, I'm looking at you!
			SyncNetworkHandler.instance.sendToServer(SyncNetworkHandler.preparePacket(pos));
		}
		return false;
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		if (sender.isDead || sender.getDistanceSqToCenter(pos) > 30) return;
		//that player is looking at me, let's make it see state changes.
		if (watching == null) watching = new HashSet<EntityPlayerMP>(2);
		if (watching.add(sender)) markDirty(SAVE);
	}

}
