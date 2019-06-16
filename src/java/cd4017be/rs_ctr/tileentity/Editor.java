package cd4017be.rs_ctr.tileentity;

import static cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet.INS_SET;

import java.io.File;
import java.util.HashMap;

import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.GlitchSaveSlot;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.network.StateSynchronizer.Builder;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.ItemKey;
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.circuit.CircuitCompiler;
import cd4017be.rs_ctr.circuit.CompiledCircuit;
import cd4017be.rs_ctr.gui.CircuitEditor;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;
import cd4017be.rscpl.editor.Schematic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public class Editor extends BaseTileEntity implements IGuiHandlerTile, IStateInteractionHandler {

	public static int CAPACITY = 256;
	public static final HashMap<ItemKey, int[]> RECIPES = new HashMap<>();
	static {
		RECIPES.put(new ItemKey(new ItemStack(Items.REDSTONE)), new int[] {2, 0, 0});
		RECIPES.put(new ItemKey(new ItemStack(Blocks.REDSTONE_BLOCK)), new int[] {18, 0, 0});
		RECIPES.put(new ItemKey(new ItemStack(Items.GOLD_NUGGET)), new int[] {0, 0 ,2});
		RECIPES.put(new ItemKey(new ItemStack(Items.GOLD_INGOT)), new int[] {0, 0, 18});
		RECIPES.put(new ItemKey(new ItemStack(Blocks.GOLD_BLOCK)), new int[] {0, 0, 162});
		RECIPES.put(new ItemKey(new ItemStack(Items.QUARTZ)), new int[] {0, 4, 0});
		RECIPES.put(new ItemKey(new ItemStack(Blocks.QUARTZ_BLOCK)), new int[] {0, 16, 0});
	}

	public File lastFile;

	public final Schematic schematic = new Schematic(INS_SET, 60, 35);
	public String name = "";
	/** 0:av A, 1:av B, 2:av C, 3:req A, 4:req B, 5:req C, 6:last Error */
	public int[] ingreds = {0,0,0, 0,0,0, InvalidSchematicException.NO_ERROR};
	public ItemStack inventory = ItemStack.EMPTY;

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SYNC) schematic.getChanges(nbt);
		else {
			nbt.setString("name", name);
			nbt.setIntArray("ingred", ingreds);
			if (!inventory.isEmpty())
				nbt.setTag("inv", inventory.writeToNBT(new NBTTagCompound()));
			ByteBuf buf = Unpooled.buffer();
			schematic.serialize(buf);
			byte[] data = new byte[buf.writerIndex()];
			buf.readBytes(data);
			nbt.setByteArray("schematic", data);
		}
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode == SYNC) {
			schematic.applyChanges(nbt);
			computeCost();
		} else {
			name = nbt.getString("name");
			{int[] buf = nbt.getIntArray("ingred");
			System.arraycopy(buf, 0, ingreds, 0, buf.length < 7 ? buf.length : 7);}
			if (nbt.hasKey("inv", NBT.TAG_COMPOUND))
				inventory = new ItemStack(nbt.getCompoundTag("inv"));
			else inventory = ItemStack.EMPTY;
			if (nbt.hasKey("schematic", NBT.TAG_BYTE_ARRAY))
				schematic.deserialize(Unpooled.wrappedBuffer(nbt.getByteArray("schematic")));
			else schematic.clear();
			schematic.resetSync();
		}
	}

	private void computeCost() {
		int a = 0, b = 0, c = 0;
		for (Gate<?> op : schematic.operators)
			if (op != null) {
				int i = INS_SET.getCost(op.type);
				a += i >> 16 & 0xff;
				b += i >> 8 & 0xff;
				c += i & 0xff;
			}
		ingreds[3] = a;
		ingreds[4] = b;
		ingreds[5] = c;
	}

	public static final int
		NO_CIRCUITBOARD = 32,
		MISSING_RESOURCE = 33,
		MISSING_IO_LABEL = 64;

	void compile() throws InvalidSchematicException {
		ItemStack stack = inventory;
		if (stack.getItem() != Objects.processor)
			throw new InvalidSchematicException(NO_CIRCUITBOARD, null, 0);
		computeCost();
		/*int[] cost = ingreds.clone(), ingr;
		int n = stack.getCount();
		if (stack.hasTagCompound())
			ingr = Arrays.copyOf(stack.getTagCompound().getIntArray("ingr"), 3);
		else ingr = new int[3];
		for (int i = 0; i < 3; i++)
			if (cost[i] < (cost[i+3] -= ingr[i]) * n)
				throw new InvalidSchematicException(MISSING_RESOURCE, null, i);
		*/
		CompiledCircuit cc = CircuitCompiler.INSTANCE.compile(schematic.operators);
		NBTTagCompound nbt = cc.serializeNBT();
		/*for (int i = 0, c; i < 3; i++)
			if ((c = cost[i + 3]) > 0) {
				ingr[i] += c;
				ingreds[i] -= c * n;
			}
		nbt.setIntArray("ingr", ingr);*/
		nbt.setString("name", name);
		stack.setTagCompound(nbt);
		if (cc.compileWarning != null) throw cc.compileWarning;
	}

	private void putItem(ItemStack stack, int slot) {
		inventory = stack;
		int n = stack.getCount();
		if (world.isRemote || n <= 0) return;
		ingreds[6] = InvalidSchematicException.NO_ERROR;
		markDirty();
		if (stack.getItem() == Objects.circuitboard && stack.hasTagCompound()) {
			NBTTagCompound nbt = stack.getTagCompound();
			int[] ig = nbt.getIntArray("ingr");
			boolean empty = false;
			for (int i = 0; i < 3; i++) {
				int x = ig[i], d = (CAPACITY - ingreds[i]) / n;
				if (x > 0 && d > 0) {
					if (d > x) d = x;
					ig[i] = x -= d;
					ingreds[i] += d * n;
				}
				empty |= x <= 0;
			}
			if (empty) stack.setTagCompound(null);
			else nbt.getKeySet().removeIf((key)-> !key.equals("ingr"));
		} else {
			int[] c = RECIPES.get(new ItemKey(stack));
			if (c == null) return;
			for (int i = 0; i < 3; i++)
				if (CAPACITY - ingreds[i] < c[i] * n) return;
			for (int i = 0; i < 3; i++)
				ingreds[i] += c[i] * n;
			inventory = ItemStack.EMPTY;
		}
	}

	@Override
	protected void setupData() {
		schematic.server = !world.isRemote;
	}

	private static final Builder ssb = StateSynchronizer.builder().addMulFix(4, 7).addVar(1);

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		AdvancedContainer cont = new AdvancedContainer(this, ssb.build(world.isRemote), player);
		LinkedInventory inv = new LinkedInventory(1, 64, (s)-> inventory, this::putItem);
		cont.addItemSlot(new GlitchSaveSlot(inv, 0, 174, 232, false), false);
		cont.addPlayerInventory(8, 174);
		if (world.isRemote) schematic.modified = true;
		else computeCost();
		return cont;
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.writeIntArray(ingreds).endFixed()
		.putAll(name);
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		ingreds = state.get(ingreds);
		name = state.get(name);
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return !unloaded && !player.isDead && player.getDistanceSqToCenter(pos) < 256.0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public CircuitEditor getGuiScreen(EntityPlayer player, int id) {
		return new CircuitEditor(this, player);
	}

	public static final byte A_NEW = -1, A_LOAD = -2, A_SAVE = -3, A_COMPILE = -4, A_NAME = -5;

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		byte cmd = pkt.readByte();
		ingreds[6] = InvalidSchematicException.NO_ERROR;
		switch(cmd) {
		case A_NEW:
			schematic.clear();
			name = "";
			break;
		case A_LOAD:
			pkt.readInt();//magic
			name = pkt.readCharSequence(pkt.readUnsignedByte(), Utils.UTF8).toString();
			schematic.deserialize(pkt);
			sender.sendMessage(new TextComponentTranslation("msg.rs_ctr.load_succ"));
			break;
		case A_SAVE: return;
		case A_COMPILE:
			try {
				compile();
			} catch (InvalidSchematicException e) {
				ingreds[6] = e.compact();
			} return;
		case A_NAME: name = pkt.readString(64); return;
		default: if (!schematic.handleUserInput(cmd, pkt)) return;
		}
		markDirty(SYNC);
	}

}
