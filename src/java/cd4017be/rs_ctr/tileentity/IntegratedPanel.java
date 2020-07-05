package cd4017be.rs_ctr.tileentity;

import static cd4017be.rs_ctr.tileentity.Panel.*;
import static cd4017be.rs_ctr.tileentity.Processor.BURNOUT_INTERVAL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cd4017be.api.rs_ctr.com.*;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.network.*;
import cd4017be.lib.render.Util;
import cd4017be.lib.util.*;
import cd4017be.rs_ctr.circuit.*;
import cd4017be.rs_ctr.gui.BlockButton;
import cd4017be.rs_ctr.gui.GuiProcessor;
import cd4017be.rs_ctr.render.ISpecialRenderComp;
import cd4017be.rs_ctr.tileentity.part.Module;
import cd4017be.rs_ctr.tileentity.part.Module.IPanel;
import cd4017be.rscpl.util.StateBuffer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/** 
 * @author CD4017BE */
public class IntegratedPanel extends WallMountGate
implements IUpdatable, IServerPacketReceiver, IPlayerPacketReceiver, IProcessor, IPanel, ISpecialRenderComp,
ITilePlaceHarvest, IStateInteractionHandler, IGuiHandlerTile {

	public static final Pattern LINK_INTERNAL = Pattern.compile("([0-3]{2}).*");

	ItemStack[] ingreds = new ItemStack[0];
	int[] stats = new int[6];
	String name = "";
	BlockButton coreBtn = new BlockButton(null, ()-> null, ()-> name + "\n" + getError()) {
		@Override
		public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
			if (!hit)
				GuiNetworkHandler.openBlockGui(player, pos, 256);
			return true;
		}
	}.setSize(0.25F, 0.25F);
	public Circuit circuit;
	SignalHandler[] callbacks;
	EnergyHandler energySup;
	private long lastTick = 0;
	public int energy, cap, usage, gain;
	public byte tick;
	public String lastError;
	DelayedSignal delayed;
	String[] keys = new String[0], labels = new String[0];

	Orientation oldO = Orientation.N;
	Module[] modules = new Module[0];

	{ports = new MountedPort[] {new MountedPort(this, 0, EnergyHandler.class, true)};}

	@Override
	public void process() {
		tick = 0;
		if (unloaded) return;
		long t = world.getTotalWorldTime();
		if (lastTick > t) return;
		if (usage > 0) {
			int e = energy - usage + (int)(t - lastTick) * gain;
			if (e >= 0) energy = e <= cap ? e : cap;
			else if (energySup != null && (e -= energySup.changeEnergy(e - cap, false)) >= 0) energy = e;
			else {
				doBurnout(false);
				lastError = "power depleted";
				energy = BURNOUT_INTERVAL * gain;
				markDirty(SYNC);
				return;
			}
		}
		lastTick = t;
		try {
			int d = circuit.tick();
			for (; delayed != null; delayed = delayed.next, d |= 1)
				circuit.inputs[delayed.id] = delayed.value;
			if ((d & 1) != 0) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			}
			d >>>= 1;
			for (int i = 0; d != 0; i++, d >>>= 1)
				if ((d & 1) != 0 && callbacks[i] != null)
					callbacks[i].updateSignal(circuit.outputs[i]);
			if (lastError != null) {
				lastError = null;
				markDirty(SYNC);
			}
		} catch(Throwable e) {
			lastError = circuit.processError(e, this);
			if (lastError == null) {
				lastError = "BUG! see log";
				doBurnout(true);
			} else doBurnout(false);
			markDirty(SYNC);
		}
	}

	public void doBurnout(boolean hard) {
		Random rand = world.rand;
		world.playSound((EntityPlayer)null, pos, hard ? SoundEvents.ENTITY_GENERIC_EXPLODE : SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.BLOCKS, hard ? 5F : 0.5F, 2.6F + (rand.nextFloat() - rand.nextFloat()) * 0.8F);
		for (int i = 0; i < 5; ++i) {
			double d0 = (double)pos.getX() + rand.nextDouble() * 0.6D + 0.2D;
			double d1 = (double)pos.getY() + rand.nextDouble() * 0.6D + 0.2D;
			double d2 = (double)pos.getZ() + rand.nextDouble() * 0.6D + 0.2D;
			world.spawnParticle(hard ? EnumParticleTypes.SMOKE_LARGE : EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
		}
		lastTick = hard ? Long.MAX_VALUE : world.getTotalWorldTime() + BURNOUT_INTERVAL;
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		int[] inputs = circuit.inputs;
		return circuit.isInterrupt(pin) ?
			(val)-> {
				if (inputs[pin] == val) return;
				if (tick == 0) {
					tick = TickRegistry.TICK;
					TickRegistry.schedule(this);
				} else if (tick != TickRegistry.TICK) {
					delayed = new DelayedSignal(pin, val, delayed);
					return;
				}
				inputs[pin] = val;
			} : (val)-> inputs[pin] = val;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		pin -= circuit.inputs.length;
		if (pin == circuit.outputs.length)
			energySup = callback instanceof EnergyHandler ? (EnergyHandler)callback : null;
		else {
			SignalHandler scb = callback instanceof SignalHandler ? (SignalHandler)callback : null;
			callbacks[pin] = scb;
			if (scb != null)
				scb.updateSignal(circuit.outputs[pin]);
		}
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).updateSignal(0);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode <= CLIENT || mode == ITEM) {
			nbt.merge(circuit.serializeNBT());
			nbt.setTag("labels", Utils.writeStringArray(labels));
			nbt.setString("name", name);
			nbt.setIntArray("stats", stats);
			if (mode != CLIENT)
				nbt.setTag("ingr", ItemFluidUtil.saveItems(ingreds));
			if (mode != ITEM)
				nbt.setInteger("energy", energy);
		} else if (mode == SYNC) {
			if (lastError != null)
				nbt.setString("err", lastError);
		}
		if (mode == SAVE) {
			nbt.setLong("burnout", lastTick);
			nbt.setBoolean("active", tick != 0);
		}
		NBTTagList list = new NBTTagList();
		for (Module m : modules)
			list.appendTag(m != null ? m.serializeNBT() : new NBTTagCompound());
		nbt.setTag("modules", list);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode <= CLIENT || mode == ITEM) {
			circuit = mode == ITEM ? new CompiledCircuit() : new UnloadedCircuit();
			circuit.deserializeNBT(nbt);
			labels = Utils.readStringArray(nbt.getTagList("labels", NBT.TAG_STRING), new String[circuit.inputs.length + circuit.outputs.length]);
			createPorts();
			name = nbt.getString("name");
			keys = circuit.getState().nbt.getKeySet().toArray(keys);
			Arrays.sort(keys);
			{int[] arr = nbt.getIntArray("stats");
			System.arraycopy(arr, 0, stats, 0, Math.min(arr.length, stats.length));}
			if (mode != CLIENT)
				ingreds = ItemFluidUtil.loadItems(nbt.getTagList("ingr", NBT.TAG_COMPOUND));
			energy = nbt.getInteger("energy");
			gain = stats[4];
			cap = stats[5];
			if ((usage = stats[0] + stats[1]) < gain)
				usage = 0;
		} else if (mode == SYNC) {
			lastError = nbt.hasKey("err", NBT.TAG_STRING) ? nbt.getString("err") : null;
		}
		NBTTagList list = nbt.getTagList("modules", NBT.TAG_COMPOUND);
		int n = list.tagCount();
		if (mode != SYNC)
			while(n > 0 && list.getCompoundTagAt(n-1).hasNoTags())
				n--;
		if (n != modules.length) modules = new Module[n];
		//Just a dummy list to keep Module.init() happy
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
			}
		}
		super.loadState(nbt, mode);
		if (mode == SAVE) {
			lastTick = nbt.getLong("burnout");
			tick = (byte)(nbt.getBoolean("active") ? 1 : 0);
		}
	}

	private void createPorts() {
		int in = circuit.inputs.length, out = circuit.outputs.length, ni = 0, no = 0;
		ports = new MountedPort[Math.min(out, 4) + Math.min(in, 4) + 1];
		for (int i = 0; i < in && ni < 4; i++) {
			String name = labels[i];
			if (LINK_INTERNAL.matcher(name).matches()) continue;
			ports[ni] = new MountedPort(this, i, SignalHandler.class, false)
			.setLocation(0.125, 0.125 + ni * 0.25, 0.5, EnumFacing.NORTH, o)
			.setName("\\" + name);
			ni++;
		}
		for (int i = 0; i < out && no < 4; i++) {
			String name = labels[in + i];
			if (LINK_INTERNAL.matcher(name).matches()) continue;
			ports[ni + no] = new MountedPort(this, in + i, SignalHandler.class, false)
			.setLocation(0.875, 0.125 + no * 0.25, 0.5, EnumFacing.NORTH, o)
			.setName("\\" + name);
			no++;
		}
		no += ni;
		ports[no] = new MountedPort(this, in + out, EnergyHandler.class, true)
		.setLocation(0.5, 0.125, 0.5, EnumFacing.NORTH, o).setName("port.rs_ctr.energy_i");
		if (++no < ports.length)
			ports = Arrays.copyOf(ports, no);
	}

	@Override
	public void onLoad() {
		if (circuit == null) circuit = new UnloadedCircuit();
		if (!world.isRemote) {
			circuit = circuit.load();
			callbacks = new SignalHandler[circuit.outputs.length];
			if (tick == 1) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			}
		}
		for (Module m : modules)
			if (m != null)
				m.onLoad(this);
		super.onLoad();
		//connect internal wires:
		if (world.isRemote) return;
		int in = circuit.inputs.length;
		for (int i = 0; i < labels.length; i++) {
			Matcher m = LINK_INTERNAL.matcher(labels[i]);
			if (!m.matches()) continue;
			int p = 1 << Integer.parseInt(m.group(1), 4);
			for (Module mod : modules) {
				if (mod == null || (mod.getBounds() & p) == 0) continue;
				if (i < in) mod.setPortCallback(getPortCallback(i));
				else setPortCallback(i, mod.getPortCallback());
				break;
			}
		}
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		if (!world.isRemote)
			for (int i = circuit.inputs.length; i < labels.length; i++)
				setPortCallback(i, null);
		for (Module m : modules) {
			if (m == null) continue;
			if (!world.isRemote)
				m.setPortCallback(null);
			m.onUnload();
		}
		watching = null;
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(coreBtn);
		for (Module m : modules)
			if (m != null)
				list.add(m);
	}

	@Override
	protected void orient(Orientation o) {
		coreBtn.setLocation(0.5, 0.375, 0.75, Orientation.values()[o.ordinal()^8]);
		super.orient(o);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt == null) return;
		loadState(nbt, ITEM);
		energy = cap;
		lastTick = world.getTotalWorldTime();
		tick = 1;
		unloaded = true; //avoid log warnings
		onLoad();
		if (world.isRemote) return;
		markDirty(REDRAW);
		if (entity instanceof EntityPlayerMP)
			handlePlayerPacket(null, (EntityPlayerMP)entity);
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		return makeDefaultDrops();
	}

	@Override
	public String getName() {
		return "\\" + name;
	}

	public String getError() {
		return lastError == null ? "\u00a7ano error" : "\u00a7c" + lastError;
	}

	public String[] getIOLabels() {
		return labels;
	}

	@Override
	public double getExhaustion() {
		return (double)(cap - energy) / (double)cap * 100D;
	}

	@Override
	public Circuit getCircuit() {
		return circuit;
	}

	@Override
	public int getClockState() {
		return tick;
	}

	@Override
	public Container getContainer(EntityPlayer player, int id) {
		if (id < modules.length) return modules[id].getCfgContainer(player);
		return new AdvancedContainer(this, StateSynchronizer.builder()
			.addFix(1, 4)
			.addMulFix(4, circuit.inputs.length + circuit.outputs.length)
			.addVar(keys.length)
			.build(world.isRemote), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGuiScreen(EntityPlayer player, int id) {
		if (id < modules.length) return modules[id].getCfgScreen(player);
		return new GuiProcessor(this, (AdvancedContainer)getContainer(player, id));
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.writeInt(tick > 0 ? 1 : 0).writeInt(Math.min(cap, energy + (int)(world.getTotalWorldTime() - lastTick - 1) * gain));
		state.writeIntArray(circuit.inputs).writeIntArray(circuit.outputs).endFixed();
		NBTTagCompound nbt = circuit.getState().nbt;
		for (String key : keys) {
			Utils.writeTag(state.buffer, nbt.getTag(key));
			state.put();
		}
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		tick = (byte)state.get(tick);
		energy = state.get(energy);
		circuit.inputs = state.get(circuit.inputs);
		circuit.outputs = state.get(circuit.outputs);
		NBTTagCompound nbt = circuit.getState().nbt;
		for (String key : keys)
			if (state.next())
				nbt.setTag(key, Utils.readTag(state.buffer, nbt.getTagId(key)));
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return canPlayerAccessUI(player);
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		int var = pkt.readUnsignedByte();
		if (var == 255) {
			if (tick == 0) {
				tick = TickRegistry.TICK;
				TickRegistry.schedule(this);
			}
			return;
		}
		String key;
		if (var >= keys.length || (key = keys[var]) == null) return;
		StateBuffer state = circuit.getState();
		state.nbt.setTag(key, Utils.readTag(pkt, state.nbt.getTagId(key)));
		circuit.setState(state);
	}

	@Override
	public Object getState(int id) {
		return id < circuit.inputs.length ? circuit.inputs[id]
			: (id -= circuit.inputs.length) < circuit.outputs.length ? circuit.outputs[id]
			: null;
	}

	@Override
	public Port getPort(int pin) {
		Port p = super.getPort(pin);
		if (p == null)
			for (Port port : ports)
				if (port.pin == pin)
					return port;
		return p;
	}

	//state synchronization
	Set<EntityPlayerMP> watching = null;
	boolean update;

	@Override
	public void remove(int id) {
		Module m = modules[id];
		if (m != null) {
			int b = m.getBounds();
			m.setPortCallback(null);
			for (int in = circuit.inputs.length, i = 0; i < labels.length; i++) {
				Matcher mt = LINK_INTERNAL.matcher(labels[i]);
				if (!mt.matches()) continue;
				int p = 1 << Integer.parseInt(mt.group(1), 4);
				if ((b & p) == 0) continue;
				if (i < in) resetPin(i);
				else setPortCallback(i, null);
			}
			m.onUnload();
			modules[id] = null;
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
		m.onLoad(this);
		if (!list.isEmpty()) {
			int in = circuit.inputs.length;
			for (i = 0; i < labels.length; i++) {
				Matcher mt = LINK_INTERNAL.matcher(labels[i]);
				if (!mt.matches()) continue;
				int p = 1 << Integer.parseInt(mt.group(1), 4);
				if ((p & b) == 0) continue;
				if (i < in) m.setPortCallback(getPortCallback(i));
				else setPortCallback(i, m.getPortCallback());
			}
		}
		markDirty(REDRAW);
		gui = null;
		return true;
	}

	@Override
	public void updateDisplay() {
		if (watching != null && !update) {
			update = true;
			TickRegistry.schedule(this::syncPanelState);
		}
		markDirty(SAVE);
	}

	public void syncPanelState() {
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

	int cachedLight = -1;

	@Override
	public int frontLight() {
		int l = cachedLight;
		if (l >= 0) return l;
		return cachedLight = world.getCombinedLight(pos.offset(o.back), 0);
	}

}
