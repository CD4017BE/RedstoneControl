package cd4017be.rscpl.editor;

import java.util.ArrayList;
import java.util.BitSet;

import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.Main;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * @author CD4017BE
 *
 */
public class Schematic {

	public final InstructionSet INS_SET;
	public final BoundingBox2D<Gate> BOARD_AREA;
	public ArrayList<Gate> operators = new ArrayList<>();
	/** idx<<1 | 0: gate main data modified<br>idx<<1 | 1: gate configuration modified */
	private BitSet toSync = new BitSet();
	public boolean modified, server;

	public Schematic(InstructionSet insSet, int width, int height) {
		this.INS_SET = insSet;
		this.BOARD_AREA = new BoundingBox2D<>(null, 0, 0, width - 1, height);
	}

	public void resetSync() {
		modified |= !toSync.isEmpty();
		toSync.clear();
	}

	public void clear() {
		if (server)
			for (int i = 0, l = operators.size(); i < l; i++)
				if (operators.get(i) != null)
					toSync.set(i << 1);
		operators.clear();
	}

	/**
	 * @param data Format: {B_gateCount, {B_gateId, 2B_size, B_extra[size]}[gateCount]}
	 */
	public void deserialize(ByteBuf data) {
		clear();
		int n = data.readUnsignedByte();
		for (int i = 0; i < n; i++) {
			GateType t = INS_SET.get(data.readUnsignedByte());
			int l = data.readUnsignedShort();
			int p = l + data.readerIndex();
			if (t != null) {
				Gate g = t.newGate(operators.size());
				g.read(data);
				if (g instanceof ConfigurableGate)
					((ConfigurableGate)g).readCfg(data);
				operators.add(g);
			}
			if (data.readerIndex() != p) {
				Main.LOG.warn("corrupted data in circuit schematic:\ntype = {}, exp_size = {}, read_size = {}", t, l, l - p + data.readerIndex());
				data.readerIndex(p);
			}
		}
		for (Gate g : operators) g.reconnect(this::get);
		if (server) toSync.set(0, n << 1);
	}

	/**
	 * @param data Format: {B_gateCount, {B_gateId, 2B_size, B_extra[size]}[gateCount]}
	 */
	public void serialize(ByteBuf data) {
		int i = data.writerIndex(), n = 0;
		data.writeByte(0);
		for (Gate g : operators)
			if (g != null) {
				data.writeByte(INS_SET.id(g.type));
				int j = data.writerIndex();
				data.writeShort(0);
				g.write(data);
				if (g instanceof ConfigurableGate)
					((ConfigurableGate)g).writeCfg(data);
				data.setShort(j, data.writerIndex() - j - 2);
				n++;
			}
		data.setByte(i, n);
	}

	public void getChanges(NBTTagCompound nbt, boolean all) {
		NBTTagList list = new NBTTagList();
		ByteBuf buf = Unpooled.buffer();
		int j = -1, l = operators.size();
		while(l > 0 && operators.get(l - 1) == null) l--;
		l <<= 1;
		while(all ? ++j < l : (j = toSync.nextSetBit(j + 1)) >= 0) {
			NBTTagCompound tag = new NBTTagCompound();
			int i = j >> 1;
			tag.setByte("i", (byte)i);
			Gate op = get(i);
			if (op != null) {
				tag.setByte("t", (byte)INS_SET.id(op.type));
				if ((j & 1) == 0) {
					op.write(buf);
					byte[] arr = new byte[buf.writerIndex()];
					buf.readBytes(arr);
					tag.setByteArray("d", arr);
					buf.clear();
				}
				if ((all || (j & 1) != 0 || toSync.get(j | 1)) && op instanceof ConfigurableGate) {
					((ConfigurableGate)op).writeCfg(buf);
					byte[] arr = new byte[buf.writerIndex()];
					buf.readBytes(arr);
					tag.setByteArray("c", arr);
					buf.clear();
				}
			}
			j |= 1;
			list.appendTag(tag);
		}
		nbt.setTag("d", list);
		if (!all) toSync.clear();
	}

	public void applyChanges(NBTTagCompound nbt) {
		NBTTagList list = nbt.getTagList("d", NBT.TAG_COMPOUND);
		ByteBuf buf = Unpooled.buffer();
		for (NBTBase bt : list) {
			NBTTagCompound tag = (NBTTagCompound)bt;
			int i = tag.getByte("i") & 0xff;
			Gate op = get(i);
			GateType t = tag.hasKey("t", NBT.TAG_BYTE) ? INS_SET.get(tag.getByte("t")) : null;
			if ((op == null ? null : op.type) != t) {
				if (op != null) op.remove();
				if (t != null) {
					while (i > operators.size()) operators.add(null);
					op = t.newGate(i);
					if (i < operators.size()) operators.set(i, op);
					else operators.add(op);
				} else {
					op = null;
					if (i < operators.size()) operators.set(i, null);
				}
			}
			if (op != null && tag.hasKey("d", NBT.TAG_BYTE_ARRAY)) {
				buf.writeBytes(tag.getByteArray("d"));
				op.read(buf);
				buf.clear();
			}
			if (op instanceof ConfigurableGate && tag.hasKey("c", NBT.TAG_BYTE_ARRAY)) {
				buf.writeBytes(tag.getByteArray("c"));
				((ConfigurableGate)op).readCfg(buf);
				buf.clear();
			}
		}
		for (NBTBase tag : list) {
			Gate op = get(((NBTTagCompound)tag).getByte("i") & 0xff);
			if (op != null)
				op.reconnect(this::get);
		}
		modified = true;
	}

	public Gate get(int idx) {
		if (idx < operators.size())
			return operators.get(idx);
		return null;
	}

	public BoundingBox2D<Gate> getCollision(BoundingBox2D<Gate> box) {
		if (!box.enclosedBy(BOARD_AREA)) return BOARD_AREA;
		BoundingBox2D<Gate> box1;
		for (Gate op1 : operators)
			if (op1 != null && (box1 = op1.getBounds()).overlapsWith(box))
				return box1;
		return null;
	}

	public static final byte
			ADD_GATE = 0, REM_GATE = 1, MOVE_GATE = 2,
			CONNECT = 3, SET_LABEL = 4, SET_VALUE = 5,
			INS_TRACE = 8, REM_TRACE = 9, MOVE_TRACE = 10;

	public boolean handleUserInput(byte actionID, ByteBuf data) {
		switch(actionID) {
		case ADD_GATE: {
			int i = operators.indexOf(null);
			if (i < 0) i = operators.size();
			if (i >= 256) return false;
			Gate op = INS_SET.newGate(data.readByte(), i);
			if (op == null) return false;
			op.setPosition(data.readUnsignedByte(), data.readUnsignedByte());
			if (getCollision(op.getBounds()) != null) return false;
			if (i == operators.size()) operators.add(op);
			else operators.set(i, op);
			toSync.set(i << 1);
		}	return true;
		case REM_GATE: {
			int i = data.readUnsignedByte();
			Gate op = get(i);
			if (op == null) return false;
			op.remove();
			operators.set(i, null);
			toSync.set(i << 1);
		}	return true;
		case MOVE_GATE: {
			int i = data.readUnsignedByte();
			Gate op = get(i);
			if (op == null) return false;
			int prevX = op.rasterX, prevY = op.rasterY;
			op.setPosition(data.readUnsignedByte(), data.readUnsignedByte());
			if (getCollision(op.getBounds()) != null) {
				op.rasterX = prevX;
				op.rasterY = prevY;
				return false;
			}
			toSync.set(i << 1);
		}	return true;
		case CONNECT: {
			int i = data.readUnsignedByte();
			Gate op = get(i), op1 = get(data.readUnsignedByte());
			if (op == null) return false;
			int pins = data.readUnsignedByte();
			if ((pins & 15) >= op.inputCount()) return false;
			int trace = data.readUnsignedByte();
			op.setInput(pins & 15, op1 != null ? op1.outputs[pins >> 4] : null);
			pins &= 15;
			if (trace == 0) op.traces[pins] = null;
			else for (TraceNode tn = op.traces[pins]; tn != null; tn = tn.next)
				if (--trace == 0) tn.next = null;
			toSync.set(i << 1);
		}	return true;
		case SET_LABEL: {
			int i = data.readUnsignedByte();
			Gate op = get(i);
			if (op == null) return false;
			op.label = data.toString(Utils.UTF8);
			data.readerIndex(data.writerIndex());
			toSync.set(i << 1);
		}	return true;
		case SET_VALUE: {
			int i = data.readUnsignedByte();
			Gate op = get(i);
			if (!(op instanceof ConfigurableGate)) return false;
			((ConfigurableGate)op).readCfg(data);
			toSync.set(i << 1 | 1);
		}	return true;
		case INS_TRACE: {
			int i = data.readUnsignedByte();
			Gate op = get(i);
			if (op == null) return false;
			int p = data.readUnsignedByte(), t = p >> 4; p &= 15;
			if (p >= op.inputCount()) return false;
			TraceNode tn = new TraceNode(op, p);
			tn.rasterX = data.readUnsignedByte();
			tn.rasterY = data.readUnsignedByte();
			if (t == 0) {
				tn.next = op.traces[p];
				op.traces[p] = tn;
			} else {
				TraceNode tn1 = op.getTrace(p, t - 1);
				if (tn1 == null) return false;
				tn.next = tn1.next;
				tn1.next = tn;
			}
			toSync.set(i << 1);
		}	return true;
		case REM_TRACE: {
			int i = data.readUnsignedByte();
			Gate op = get(i);
			if (op == null) return false;
			int p = data.readUnsignedByte();
			if (p >= op.inputCount()) return false;
			int t = data.readUnsignedByte(), t1 = t >> 4; t &= 15;
			if (t == 0) op.traces[p] = op.getTrace(p, t1);
			else {
				TraceNode tn = op.getTrace(p, t - 1);
				if (tn == null || t1 < t) return false;
				tn.next = op.getTrace(p, t1);
			}
			toSync.set(i << 1);
		}	return true;
		case MOVE_TRACE:{
			int i = data.readUnsignedByte();
			Gate op = get(i);
			if (op == null) return false;
			int p = data.readUnsignedByte(), t = p >> 4; p &= 15;
			if (p >= op.inputCount()) return false;
			int x = data.readUnsignedByte(), y = data.readUnsignedByte();
			TraceNode tn = op.getTrace(p, t);
			if (tn == null) return false;
			tn.rasterX = x;
			tn.rasterY = y;
			toSync.set(i << 1);
		}	return true;
		default: return false;
		}
	}


}
