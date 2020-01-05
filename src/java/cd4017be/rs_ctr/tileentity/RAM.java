package cd4017be.rs_ctr.tileentity;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.gui.GuiRAM;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author cd4017be */
public class RAM extends WallMountGate implements SignalHandler, IGuiHandlerTile, IStateInteractionHandler, ITilePlaceHarvest {

	public int[] memory;
	public int addrMask;
	public byte mode, page;
	private SignalHandler out = SignalHandler.NOP;
	public int readIN, writeIN = -1, valueIN, valueOUT;
	private long scheduledTime;
	private boolean needWrite;
	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, SignalHandler.class, false)
			.setLocation(.125, .125, .25, EnumFacing.SOUTH).setName("port.rs_ctr.wa"),
			new MountedPort(this, 1, SignalHandler.class, false)
			.setLocation(.125, .375, .25, EnumFacing.SOUTH).setName("port.rs_ctr.wv"),
			new MountedPort(this, 2, SignalHandler.class, false)
			.setLocation(.125, .625, .25, EnumFacing.SOUTH).setName("port.rs_ctr.ra"),
			new MountedPort(this, 3, SignalHandler.class, true)
			.setLocation(.125, .875, .25, EnumFacing.SOUTH).setName("port.rs_ctr.rv")
		};
		setMode(0x70);
		memory = new int[memSize()];
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		switch(pin) {
		case 0: return this::updateAddr;
		case 1: return this::updateVal;
		default: return this;
		}
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		out = callback instanceof SignalHandler ? (SignalHandler)callback
			: SignalHandler.NOP;
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).updateSignal(pin == 1 ? 0 : -1);
	}

	@Override
	public void updateSignal(int value) {
		int v = 0;
		if(value >= 0 && value <= addrMask) {
			readIN = value;
			doWrite();
			v = get(value);
		} else readIN = -1;
		if(v != valueOUT)
			out.updateSignal(valueOUT = v);
	}

	public void updateVal(int value) {
		if(value == valueIN) return;
		doWrite();
		valueIN = value;
		scheduleWrite();
	}

	public void updateAddr(int value) {
		if (value < 0 || value > addrMask) value = -1;
		if(value == writeIN) return;
		doWrite();
		writeIN = value;
		scheduleWrite();
	}

	private void scheduleWrite() {
		if(writeIN < 0)return;
		needWrite = true;
		scheduledTime = world.getTotalWorldTime();
	}

	private void doWrite() {
		if(!needWrite || world.getTotalWorldTime() <= scheduledTime)
			return;
		needWrite = false;
		int val = valueIN;
		int addr = writeIN & addrMask, a = addr >> (mode & 3);
		switch(mode & 3) {
		case 0:
			memory[a] = val;
			break;
		case 1:
			memory[a] = (addr & 1) == 0 ? memory[a] & 0xffff0000 | val & 0xffff
				: memory[a] & 0xffff | val << 16;
			break;
		case 2:
			addr = (addr & 3) << 3;
			memory[a] = memory[a] & ~(0xff << addr) | (val & 0xff) << addr;
			break;
		default:
			addr = (addr & 7) << 2;
			memory[a] = memory[a] & ~(0xf << addr) | (val & 0xf) << addr;
		}
	}

	public int get(int addr) {
		addr &= addrMask;
		switch(mode & 3) {
		case 0:
			return memory[addr];
		case 1:
			return (addr & 1) == 0 ? memory[addr >> 1] & 0xffff
				: memory[addr >> 1] >>> 16;
		case 2:
			return memory[addr >> 2] >> ((addr & 3) << 3) & 0xff;
		default:
			return memory[addr >> 3] >> ((addr & 7) << 2) & 0xf;
		}
	}

	public void setMode(int m) {
		mode = (byte)m;
		addrMask = (1 << (m >> 4 & 15) + (m & 3)) - 1;
	}

	public int memSize() {
		return 1 << (mode >> 4 & 15);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		if (mode == SAVE || mode == ITEM) {
			scheduledTime = -1;
			doWrite();
			nbt.setIntArray("mem", memory);
			nbt.setByte("mode", this.mode);
			if (mode == SAVE) {
				nbt.setInteger("addr", writeIN);
				nbt.setInteger("val", valueIN);
				nbt.setInteger("out", valueOUT);
			}
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		if (mode == CLIENT)
			memory = new int[64];
		else if (mode == SAVE || mode == ITEM) {
			setMode(nbt.getByte("mode"));
			memory = Arrays.copyOf(nbt.getIntArray("mem"), memSize());
			if (mode == SAVE) {
				writeIN = nbt.getInteger("addr");
				valueIN = nbt.getInteger("val");
				valueOUT = nbt.getInteger("out");
				needWrite = false;
			}
		}
	}

	private static final StateSynchronizer.Builder ssb = StateSynchronizer.builder()
	.addFix(4, 4, 1, 1).addMulFix(4, 64).addVar(1);

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		AdvancedContainer c = new AdvancedContainer(this, ssb.build(world.isRemote), player);
		if (world.isRemote) {
			if (memory.length != 64) memory = new int[64];
			else Arrays.fill(memory, 0);
		}
		return c;
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		PacketBuffer b = state.buffer;
		b.writeInt(readIN).writeInt(writeIN)
		.writeByte(mode).writeByte(page);
		int m = memory.length - 1;
		for (int i = (page & 0xff) << 6, l = i + 64; i < l; i++)
			b.writeInt(i <= m ? memory[i] : 0);
		state.endFixed().putAll(ArrayUtils.EMPTY_INT_ARRAY);
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		readIN = state.get(readIN);
		writeIN = state.get(writeIN);
		setMode(state.get(mode));
		page = (byte)state.get(page);
		for (int i = 0; i < 64; i++)
			memory[i] = state.get(memory[i]);
		if (!state.next()) return;
		int l = state.buffer.readVarInt();
		if (l <= 0) return;
		byte[] mem = new byte[l << 2];
		state.buffer.readBytes(mem);
		if (world.isRemote && Minecraft.getMinecraft().currentScreen instanceof GuiRAM)
			((GuiRAM)Minecraft.getMinecraft().currentScreen).processDownload(mem);
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return canPlayerAccessUI(player);
	}

	public static final byte A_MODE = 0, A_PAGE = 1, A_SET_MEM = 2, A_DOWNLOAD = 3, A_UPLOAD = 4;

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender)
	throws Exception {
		switch(pkt.readByte()) {
		case A_MODE:
			setMode(mode & 0xf0 | pkt.readByte() & 3);
			break;
		case A_PAGE:
			page = pkt.readByte();
			if (page <= 0) page = 0;
			else if (page >= memory.length >> 6)
				page = (byte)Math.max(0, (memory.length >> 6) - 1);
			break;
		case A_SET_MEM: {
			int i = pkt.readShort() & 0xffff, v = pkt.readByte();
			int bit = (i & 7) * 4, idx = i >> 3;
			if (idx < memory.length)
				memory[idx] = v << bit | memory[idx] & ~(15 << bit);
			break;
		}
		case A_DOWNLOAD:
			if (sender.openContainer instanceof AdvancedContainer) {
				AdvancedContainer c = (AdvancedContainer)sender.openContainer;
				StateSyncServer sss = ((StateSyncServer)c.sync).begin();
				pkt = sss.buffer;
				pkt.writeVarInt(memory.length);
				for (int i : memory)
					pkt.writeIntLE(i);
				sss.set(68);
				pkt = sss.encodePacket();
				if (pkt != null)
					GuiNetworkHandler.GNH_INSTANCE.sendToPlayer(pkt, sender);
			}
			break;
		case A_UPLOAD:
			int l = pkt.readUnsignedShort();
			if (l > memory.length) return;
			for (int i = 0; i < l; i++)
				memory[i] = pkt.readIntLE();
			Arrays.fill(memory, l, memory.length, 0);
			sender.sendMessage(new TextComponentTranslation("msg.rs_ctr.import_succ"));
			break;
		default: return;
		}
		markDirty(SAVE);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiRAM getGuiScreen(EntityPlayer player, int id) {
		return new GuiRAM(this, player);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		setMode(item.getMetadata() << 4);
		if (item.hasTagCompound())
			loadState(item.getTagCompound(), ITEM);
		else memory = new int[memSize()];
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		ItemStack stack = new ItemStack(Objects.ram, 1, mode >> 4 & 15);
		NBTTagCompound nbt = new NBTTagCompound();
		storeState(nbt, ITEM);
		stack.setTagCompound(nbt);
		return Arrays.asList(stack);
	}

}
