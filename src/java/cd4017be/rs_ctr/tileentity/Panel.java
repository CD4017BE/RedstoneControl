package cd4017be.rs_ctr.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.IPlayerPacketReceiver;
import cd4017be.lib.network.IServerPacketReceiver;
import cd4017be.lib.network.SyncNetworkHandler;
import cd4017be.lib.render.Util;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.render.ISpecialRenderComp;
import cd4017be.rs_ctr.tileentity.part.Module;
import cd4017be.rs_ctr.tileentity.part.Module.IPanel;
import static cd4017be.rs_ctr.ClientProxy.t_sockets;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public class Panel extends WallMountGate implements IUpdatable, IServerPacketReceiver, IPlayerPacketReceiver, IGuiHandlerTile, IPanel, ISpecialRenderComp, IBlockRenderComp, ITilePlaceHarvest {

	public static double UPDATE_RANGE0 = 256, UPDATE_RANGE1 = UPDATE_RANGE0 * 1.2, TEXT_RANGE = 100;

	Orientation oldO = Orientation.N;
	Module[] modules = new Module[0];
	{ports = new MountedPort[0];}

	@Override
	public Port getPort(int pin) {
		Port p = super.getPort(pin);
		if (p == null)
			for (Port port : ports)
				if (port.pin == pin)
					return port;
		return p;
	}

	@Override
	public Object getPortCallback(int pin) {
		return modules[pin >> 1].getPortCallback();
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		modules[pin >> 1].setPortCallback(callback);
	}

	@Override
	protected void resetPin(int pin) {
		Module m = modules[pin >> 1];
		if (m != null)
			m.resetInput();
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		NBTTagList list = new NBTTagList();
		for (Module m : modules)
			list.appendTag(m != null ? m.serializeNBT() : new NBTTagCompound());
		nbt.setTag("modules", list);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		NBTTagList list = nbt.getTagList("modules", NBT.TAG_COMPOUND);
		int n = list.tagCount();
		if (mode != SYNC)
			while(n > 0 && list.getCompoundTagAt(n-1).hasNoTags())
				n--;
		if (n != modules.length) modules = new Module[n];
		ArrayList<MountedPort> ports = new ArrayList<>(modules.length);
		for (int i = 0; i < modules.length; i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			String id = tag.getString("id");
			Module m = modules[i];
			if (m == null || !m.id().equals(id)) {
				if (m != null) m.onUnload();
				m = Module.get(id);
				modules[i] = m;
			}
			if (m != null) {
				m.deserializeNBT(tag);
				m.init(ports, i, this);
				if (!unloaded) m.onLoad(this);
				if (mode == ITEM) m.resetInput();
			}
		}
		n = ports.size();
		this.ports = ports.toArray(this.ports.length == n ? this.ports : new MountedPort[n]);
		super.loadState(nbt, mode);
	}

	@Override
	public void onLoad() {
		for (Module m : modules)
			if (m != null)
				m.onLoad(this);
		super.onLoad();
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		for (Module m : modules)
			if (m != null)
				m.onUnload();
		watching = null;
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		for (Module m : modules)
			if (m != null)
				list.add(m);
	}

	@Override
	public ArrayList<IBlockRenderComp> getBMRComponents() {
		ArrayList<IBlockRenderComp> list = super.getBMRComponents();
		list.add(this);
		return list;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void render(List<BakedQuad> quads) {
		Orientation o = getOrientation();
		Vec3d dx = o.X.scale(MountedPort.SIZE * -2.), dy = o.Y.scale(MountedPort.SIZE * 2.), p = o.Z.scale(-0.005).subtract(dx.scale(.5)).subtract(dy.scale(.5));
		for (MountedPort port : ports) {
			int i = port.isMaster ? 4 : 0, j = port.type == SignalHandler.class ? 0 : port.type == BlockHandler.class ? 4 : port.type == EnergyHandler.class ? 8 : 12;
			quads.add(new BakedQuad(Util.texturedRect(port.pos.add(p), dx, dy, Util.getUV(t_sockets, i, j), Util.getUV(t_sockets, i + 4, j + 4), -1, 0), -1, o.front, t_sockets, true, DefaultVertexFormats.BLOCK));
		}
	}

	//state synchronization
	Set<EntityPlayerMP> watching = null;
	boolean update;

	@Override
	public void remove(int id) {
		int l = ports.length;
		for (int i = 0; i < l; i++) {
			MountedPort port = ports[i];
			if (port.pin >> 1 != id) continue;
			port.setConnector(null, null);
			ports[i] = ports[--l];
		}
		Module m = modules[id];
		if (m != null) {
			m.onUnload();
			modules[id] = null;
		}
		if (l < ports.length) {
			ports = Arrays.copyOf(ports, l);
			Arrays.sort(ports);
		}
		gui = null;
		markDirty(REDRAW);
	}

	@Override
	public boolean add(Module m) {
		int b = m.getBounds();
		int i = -1;
		for (int j = 0; j < modules.length; j++) {
			Module mod = modules[j];
			if (mod != null) {
				if ((mod.getBounds() & b) != 0)
					return false;
			} else if (i < 0) i = j;
		}
		if (i < 0) {
			i = modules.length;
			modules = Arrays.copyOf(modules, i + 1);
		}
		modules[i] = m;
		ArrayList<MountedPort> list = new ArrayList<>();
		m.init(list, i, this);
		if (!list.isEmpty()) {
			i = ports.length;
			ports = Arrays.copyOf(ports, i + list.size());
			for (MountedPort port : list)
				ports[i++] = port;
			Arrays.sort(ports);
		}
		m.onLoad(this);
		markDirty(REDRAW);
		gui = null;
		return true;
	}

	@Override
	public void updateDisplay() {
		if (watching != null && !update) {
			update = true;
			TickRegistry.schedule(this);
		}
		markDirty(SAVE);
	}

	@Override
	public void process() {
		update = false;
		if (watching == null) return;
		for (Iterator<EntityPlayerMP> it = watching.iterator(); it.hasNext();) {
			EntityPlayerMP player = it.next();
			if (player.isDead || player.getDistanceSqToCenter(pos) > UPDATE_RANGE1)
				it.remove(); //player is not looking at me anymore
		}
		if (watching.isEmpty()) watching = null;
		else {
			PacketBuffer pkt = SyncNetworkHandler.preparePacket(pos);
			for (Module m : modules)
				if (m != null)
					m.writeSync(pkt, false);
			SyncNetworkHandler.instance.sendToPlayers(pkt, watching);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleServerPacket(PacketBuffer pkt) throws Exception {
		for (Module m : modules)
			if (m != null)
				m.readSync(pkt);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasFastRenderer() {
		double d = Minecraft.getMinecraft().player.getDistanceSqToCenter(pos);
		if (d > UPDATE_RANGE0) watching = null;
		else if (watching == null) {
			watching = Collections.emptySet();
			//hey server block, I'm looking at you!
			SyncNetworkHandler.instance.sendToServer(SyncNetworkHandler.preparePacket(pos));
		}
		return d > TEXT_RANGE;
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, EntityPlayerMP sender) {
		if (sender.isDead || sender.getDistanceSqToCenter(pos) > UPDATE_RANGE1) return;
		//that player is looking at me, let's make it see state changes.
		if (watching == null) watching = new HashSet<EntityPlayerMP>(2);
		if (watching.add(sender)) {
			pkt = SyncNetworkHandler.preparePacket(pos);
			for (Module m : modules)
				if (m != null)
					m.writeSync(pkt, true);
			SyncNetworkHandler.instance.sendToPlayer(pkt, sender);
		}
	}

	@Override
	public World world() {
		return world;
	}

	@Override
	public Container getContainer(EntityPlayer player, int id) {
		return id < modules.length ? modules[id].getCfgContainer(player) : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGuiScreen(EntityPlayer player, int id) {
		return id < modules.length ? modules[id].getCfgScreen(player) : null;
	}

	@Override
	public void renderSpecial(double x, double y, double z, float t, FontRenderer fr) {
		GlStateManager.pushMatrix();
		Util.moveAndOrientToBlock(x, y, z, o);
		GlStateManager.translate(-.5, .5, .505);
		GlStateManager.scale(7.8125e-3, -7.8125e-3, -1);
		Util.luminate(this, o.back, 0);
		for (Module m : modules)
			if (m != null)
				m.drawText(fr);
		GlStateManager.popMatrix();
		cachedLight = -1;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (world.isRemote) return;
		if (item.hasTagCompound()) {
			loadState(item.getTagCompound(), ITEM);
			markDirty(REDRAW);
		}
		if (entity instanceof EntityPlayerMP)
			handlePlayerPacket(null, (EntityPlayerMP)entity);
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		if (modules.length != 0)
			for (Module m : modules)
				if (m != null)
					return makeDefaultDrops();
		return makeDefaultDrops(null);
	}

	int cachedLight = -1;

	@Override
	public int frontLight() {
		int l = cachedLight;
		if (l >= 0) return l;
		return cachedLight = world.getCombinedLight(pos.offset(o.back), 0);
	}

	@Override
	public Object getState(int id) {
		return modules[id >> 1].getState(id & 1);
	}

}
