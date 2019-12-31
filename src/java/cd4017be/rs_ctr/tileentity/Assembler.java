package cd4017be.rs_ctr.tileentity;

import java.util.Arrays;
import java.util.List;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IQuickTransferHandler;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.GlitchSaveSlot;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.capability.AbstractInventory;
import cd4017be.lib.capability.BasicInventory;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.network.StateSynchronizer.Builder;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;
import static cd4017be.rs_ctr.CommonProxy.*;

import cd4017be.rs_ctr.gui.GuiAssembler;
import cd4017be.rs_ctr.item.ItemProcessor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class Assembler extends BaseTileEntity implements IGuiHandlerTile, IStateInteractionHandler, ITilePlaceHarvest, IQuickTransferHandler {

	public final Inventory inv = new Inventory();
	public final BasicInventory buff = new BasicInventory(6);

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode != SAVE) return;
		if (!inv.container.isEmpty())
			nbt.setTag("chip", inv.container.writeToNBT(new NBTTagCompound()));
		nbt.setTag("buff", buff.write());
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode != SAVE) return;
		if (nbt.hasKey("chip", NBT.TAG_COMPOUND))
			inv.setStackInSlot(0, new ItemStack(nbt.getCompoundTag("chip")));
		else inv.setStackInSlot(0, ItemStack.EMPTY);
		buff.read(nbt.getTagList("buff", NBT.TAG_COMPOUND));
	}

	private static final Builder ssb = StateSynchronizer.builder().addMulFix(4, NULL.length);

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		AdvancedContainer cont = new AdvancedContainer(this, ssb.build(world.isRemote), player);
		cont.addItemSlot(new GlitchSaveSlot(inv, 0, 80, 34, false), false);
		int[] range = new int[] {4, 10};
		for (int i = 0; i < 3; i++)
			cont.addItemSlot(new GlitchSaveSlot(inv, i + 1, 53, 16 + i * 18, false).setTarget(range), false);
		range = new int[] {1, 4};
		for (int i = 0; i < 6; i++)
			cont.addItemSlot(new GlitchSaveSlot(buff, i, 8 + (i&1) * 18, 16 + (i>>1) * 18, false).setTarget(range), false);
		cont.addPlayerInventory(8, 86);
		cont.transferHandlers.add(this);
		return cont;
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.writeIntArray(inv.stats[0]).endFixed();
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		state.get(inv.stats[0]);
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return !unloaded && !player.isDead && player.getDistanceSqToCenter(pos) < 256;
	}

	@Override
	public boolean transfer(ItemStack stack, AdvancedContainer cont) {
		if (stack.getItem() instanceof ItemProcessor) {
			cont.hardInvUpdate();
			return cont.mergeItemStack(stack, 0, 1, false);
		}
		return cont.mergeItemStack(stack, 1, 4, false)
				|| cont.mergeItemStack(stack, 4, 10, false);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiAssembler getGuiScreen(EntityPlayer player, int id) {
		return new GuiAssembler(this, player);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (!inv.container.isEmpty())
			list.add(inv.container);
		buff.addToList(list);
		return list;
	}

	public class Inventory extends AbstractInventory {

		ItemProcessor item = null;
		ItemStack container = ItemStack.EMPTY;
		final ItemStack[] ingreds = Utils.init(new ItemStack[3], ItemStack.EMPTY);
		public final int[][] stats = new int[ingreds.length + 1][NULL.length];

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (slot > 0) {
				ingreds[slot - 1] = stack;
				if (item == null) return;
				int[] s = getStats(stack), main = stats[0], old = stats[slot];
				int n = stack.getCount();
				for (int i = 0; i < main.length; i++) {
					int j = s[i] * n;
					main[i] += j - old[i];
					old[i] = j;
				}
				item.storeAll(container, ingreds, main);
			} else {
				container = stack;
				if (world != null && world.isRemote) return;
				item = stack.getItem() instanceof ItemProcessor ? (ItemProcessor)stack.getItem() : null;
				if (item == null) {
					Arrays.fill(ingreds, ItemStack.EMPTY);
					for (int[] s : stats) Arrays.fill(s, 0);
				} else {
					item.loadStats(stack, stats[0]);
					item.loadIngredients(stack, ingreds);
					for (int i = 0; i < ingreds.length; i++) {
						int[] s = getStats(ingreds[i]), old = stats[i + 1];
						int n = ingreds[i].getCount();
						for (int j = 0; j < old.length; j++)
							old[j] = s[j] * n;
					}
				}
			}
		}

		@Override
		public int getSlots() {
			return stats.length;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot == 0 ? container : ingreds[slot - 1];
		}

		@Override
		public int insertAm(int slot, ItemStack item) {
			if (slot == 0) return item.getItem() instanceof ItemProcessor ? 1 : 0;
			if (item == null) return 0;
			int[] s = getStats(item);
			if (s == NULL) return 0;
			int n = s[3];
			if (n >= 0) return item.getMaxStackSize();
			return (stats[0][3] - stats[slot][3]) / -n;
		}

	}

}
