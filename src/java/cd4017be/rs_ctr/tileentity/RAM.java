package cd4017be.rs_ctr.tileentity;

import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.rs_ctr.gui.GuiRAM;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/** @author cd4017be */
public class RAM extends WallMountGate implements SignalHandler, IGuiHandlerTile {

	public int[] memory;
	private int addrMask;
	public byte mode;
	private SignalHandler out;
	private int writeIN, valueIN, valueOUT;
	public int readAddr, writeAddr;
	private long scheduledTime;
	private boolean needWrite;
	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, SignalHandler.class, false)
			.setLocation(.125, .125, .25, EnumFacing.SOUTH).setName("\\write address"),
			new MountedPort(this, 1, SignalHandler.class, false)
			.setLocation(.125, .375, .25, EnumFacing.SOUTH).setName("\\write value"),
			new MountedPort(this, 2, SignalHandler.class, false)
			.setLocation(.125, .625, .25, EnumFacing.SOUTH).setName("\\read address"),
			new MountedPort(this, 3, SignalHandler.class, true)
			.setLocation(.125, .875, .25, EnumFacing.SOUTH).setName("\\read value")
		};
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
		if(pin == 0)
			updateSignal(0);
	}

	@Override
	public void updateSignal(int value) {
		int addr = value & ~addrMask;
		int v = 0;
		if(addr == readAddr) {
			doWrite();
			v = get(value);
		}
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
		int addr = value & ~addrMask;
		addr = addr == writeAddr ? value & addrMask : -1;
		if(addr == writeIN) return;
		doWrite();
		writeIN = addr;
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
		int addr = writeIN & addrMask;
		if(mode == 0)
			memory[addr] = val;
		else if(mode == 1) {
			int a = addr >> 1;
			memory[a] = (addr & 1) == 0 ? memory[a] & 0xffff0000 | val & 0xffff
				: memory[a] & 0xffff | val << 16;
		} else {
			int a = addr >> 2;
			addr = (addr & 3) << 3;
			memory[a] = memory[a] & ~(0xff << addr) | (val & 0xff) << addr;
		}
	}

	private int get(int addr) {
		addr &= addrMask;
		if(mode == 0)
			return memory[addr];
		if(mode == 1)
			return (addr & 1) == 0 ? memory[addr >> 1] & 0xffff
				: memory[addr >> 1] >>> 16;
		return memory[addr >> 2] >> ((addr & 3) << 3) & 0xff;
	}

	public void updateAddrMask() {
		addrMask = (memory.length << mode) - 1;
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		scheduledTime = -1;
		doWrite();
		nbt.setIntArray("mem", memory);
		nbt.setByte("mode", this.mode);
		nbt.setInteger("read", readAddr);
		nbt.setInteger("write", writeAddr);
		nbt.setInteger("addr", writeIN);
		nbt.setInteger("val", valueIN);
		nbt.setInteger("out", valueOUT);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		memory = nbt.getIntArray("mem");
		this.mode = nbt.getByte("mode");
		readAddr = nbt.getInteger("read");
		writeAddr = nbt.getInteger("write");
		writeIN = nbt.getInteger("addr");
		valueIN = nbt.getInteger("val");
		valueOUT = nbt.getInteger("out");
		updateAddrMask();
		needWrite = false;
	}

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GuiRAM getGuiScreen(EntityPlayer player, int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
