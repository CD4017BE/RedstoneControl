package cd4017be.rs_ctr.item;

import static cd4017be.lib.network.GuiNetworkHandler.openHeldItemGui;
import static cd4017be.lib.util.ItemFluidUtil.createTag;
import java.lang.ref.WeakReference;
import java.util.List;
import org.lwjgl.input.Keyboard;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.*;
import cd4017be.api.rs_ctr.port.Connector.IConnectorItem;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.ITickReceiver;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.ItemInteractionHandler;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.network.*;
import cd4017be.lib.network.StateSynchronizer.Builder;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.gui.GuiRemoteKeybind;
import cd4017be.rs_ctr.port.RemoteReceiver;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class ItemRemoteControl extends BaseItem
implements IConnectorItem, IPlayerPacketReceiver.ItemPPR, IGuiHandlerItem {

	public ItemRemoteControl(String id) {
		super(id);
		setMaxStackSize(1);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void doAttach(ItemStack stack, MountedPort port, EntityPlayer player) {
		if(port.type != SignalHandler.class) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.type"));
			return;
		} else if(port.isMaster) {
			player.sendMessage(new TextComponentTranslation("msg.rs_ctr.dir_in"));
			return;
		}
		NBTTagCompound nbt = createTag(stack);
		Sender s = getSender(nbt);
		if(s != null) s.kill();
		nbt.setInteger("link", 0);
		port.setConnector(new RemoteReceiver(port), player);
		port.connect(new Sender(nbt).port);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if(player.isSneaking() || !stack.hasTagCompound())
			openHeldItemGui(player, hand, 0, 0, 0);
		else {
			NBTTagCompound nbt = stack.getTagCompound();
			boolean enabled = nbt.getBoolean("enabled");
			if(enabled) updateState(nbt, 0);
			nbt.setBoolean("enabled", !enabled);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return TooltipUtil.translate(
			this.getUnlocalizedName(item)
			+ (item.hasTagCompound() && item.getTagCompound().getBoolean("enabled") ? ".name1" : ".name0")
		);
	}

	@Override
	public void addInformation(ItemStack item, World player, List<String> list, ITooltipFlag b) {
		super.addInformation(item, player, list, b);
		NBTTagCompound nbt = item.getTagCompound();
		if(nbt != null)
			list.add(String.format("#%d <- x%08X", nbt.getInteger("link"), nbt.getInteger("state")));
	}

	@Override
	public NBTTagCompound getNBTShareTag(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt != null && nbt.hasKey("state")) {
			nbt = nbt.copy();
			nbt.removeTag("state");
		}
		return nbt;
	}

	@Override
	public void handlePlayerPacket(ItemStack stack, int slot, PacketBuffer pkt, EntityPlayerMP sender)
	throws Exception {
		updateState(createTag(stack), pkt.readInt());
	}

	private void updateState(NBTTagCompound nbt, int value) {
		if (value == nbt.getInteger("state")) return;
		nbt.setInteger("state", value);
		if(nbt.getInteger("link") == 0) return;
		Sender h = getSender(nbt);
		if(h == null) h = new Sender(nbt);
		if (!h.update) {
			h.update = true;
			//Jitter correction, so controls stay smooth under lag or high network latency.
			h.state = value;
		}
	}

	Byte2ObjectOpenHashMap<KeyState> keyStates = new Byte2ObjectOpenHashMap<>();
	long lastTick;
	
	static class KeyState {
		boolean down, requested;
		long t;
		KeyState(byte key) {
			down = Keyboard.isKeyDown(key & 0xff);
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKeyEvent(KeyInputEvent e) {
		KeyState ks = keyStates.get((byte)Keyboard.getEventKey());
		if (ks == null || ks.down == Keyboard.getEventKeyState()) return;
		ks.down = !ks.down;
		ks.t = Keyboard.getEventNanoseconds() - ks.t;
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void updateKeys(TickEvent.ClientTickEvent e) {
		if(e.phase != Phase.END) return;
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if(player == null) return;
		NonNullList<ItemStack> inv = player.inventory.mainInventory;
		for(int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.get(i);
			if(stack.getItem() != this) continue;
			NBTTagCompound nbt = stack.getTagCompound();
			if(nbt == null || !nbt.getBoolean("enabled")) continue;
			updateKeys(nbt, i);
		}
		long dt = System.nanoTime() - lastTick;
		lastTick += dt;
		for (ObjectIterator<KeyState> it = keyStates.values().iterator(); it.hasNext(); ) {
			KeyState ks = it.next();
			if (!ks.requested) it.remove();
			else if (ks.down) ks.t += dt;
			else ks.t = 0;
			ks.requested = false;
		}
	}

	@SideOnly(Side.CLIENT)
	private void updateKeys(NBTTagCompound nbt, int slot) {
		byte[] keys = nbt.getByteArray("keys");
		int newPressed = 0;
		for(int i = 0; i < keys.length; i++) {
			byte k = keys[i];
			if (k == Keyboard.KEY_NONE) continue;
			KeyState ks = keyStates.computeIfAbsent(k, KeyState::new);
			ks.requested = true;
			if(ks.down || ks.t > 0L)
				newPressed |= 1 << i;
		}
		if(newPressed == nbt.getInteger("state")) return;
		nbt.setInteger("state", newPressed);
		PacketBuffer pkt = SyncNetworkHandler.preparePacket(slot);
		pkt.writeInt(newPressed);
		SyncNetworkHandler.instance.sendToServer(pkt);
	}

	@Override
	public AdvancedContainer getContainer(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z) {
		return new StateInteractionHandler(slot).createContainer(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGuiScreen(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z) {
		return new GuiRemoteKeybind(new StateInteractionHandler(slot), player);
	}

	private static Sender getSender(NBTTagCompound nbt) {
		Link l = Link.of(nbt.getInteger("link"));
		if(l == null) return null;
		Port p = l.source();
		if(p == null || !(p.owner instanceof Sender)) return null;
		return ((Sender)p.owner).refresh(nbt);
	}


	public static class Sender implements IPortProvider, ITickReceiver {

		private final Port port = new Port(this, 0, SignalHandler.class, true);
		private WeakReference<NBTTagCompound> ref;
		private SignalHandler callback;
		private int state;
		private boolean update;

		private Sender(NBTTagCompound nbt) {
			ref = new WeakReference<>(nbt);
			state = nbt.getInteger("state");
			TickRegistry.instance.add(this);
			port.deserializeNBT(nbt);
			port.onLoad();
		}

		private Sender refresh(NBTTagCompound nbt) {
			if(ref.get() != nbt)
				ref = new WeakReference<>(nbt);
			return this;
		}

		@Override
		public Port getPort(int pin) {
			return port;
		}

		@Override
		public Object getPortCallback(int pin) {
			return this;
		}

		@Override
		public void setPortCallback(int pin, Object callback) {
			this.callback = callback instanceof SignalHandler ? (SignalHandler)callback : SignalHandler.NOP;
			update = true;
		}

		@Override
		public void onPortModified(Port port, int event) {
			NBTTagCompound nbt = ref.get();
			if(nbt == null) return;
			nbt.setInteger("link", port.getLink());
			if(event == E_DISCONNECT) ref.clear();
		}

		@Override
		public boolean tick() {
			NBTTagCompound nbt = ref.get();
			if(nbt == null) {
				port.onUnload();
				return false;
			}
			if(update) {
				callback.updateSignal(state);
				int s = nbt.getInteger("state");
				if (s == state) update = false;
				else state = s;
			}
			return true;
		}

		public void kill() {
			Link l = Link.of(port.getLink());
			if(l != null && l.sink() instanceof MountedPort)
				((MountedPort)l.sink()).setConnector(null, null);
			port.disconnect();
			ref.clear();
		}

	}


	public static class StateInteractionHandler extends ItemInteractionHandler {

		public final byte[] keys = new byte[32];

		public StateInteractionHandler(int slot) {
			super(Objects.remote, slot);
		}

		@Override
		public void writeState(StateSyncServer state, AdvancedContainer cont) {
			state.buffer.writeBytes(getKeybinds(cont.player));
			state.endFixed();
		}

		@Override
		public void readState(StateSyncClient state, AdvancedContainer cont) {
			state.get(keys);
		}

		@Override
		protected void initSync(Builder sb) {
			sb.addMulFix(1, 32);
		}

		@Override
		public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
			int i = pkt.readUnsignedByte();
			if(i >= 32) return;
			getKeybinds(sender)[i] = pkt.readByte();
		}

		private byte[] getKeybinds(EntityPlayer player) {
			NBTTagCompound nbt = getNBT(player);
			byte[] arr = nbt.getByteArray("keys");
			if(arr.length != 32) nbt.setByteArray("keys", arr = keys);
			return arr;
		}

	}

}
