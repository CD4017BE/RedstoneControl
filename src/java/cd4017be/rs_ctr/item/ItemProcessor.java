package cd4017be.rs_ctr.item;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class ItemProcessor extends BaseItemBlock {

	final int maxIn, maxOut;

	public ItemProcessor(Block id, int in, int out) {
		super(id);
		this.maxIn = in;
		this.maxOut = out;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		use_name: {
			if (!item.hasTagCompound()) break use_name;
			String name = item.getTagCompound().getString("name");
			if (name.isEmpty()) break use_name;
			return "\u00a7o" + name;
		}
		return super.getItemStackDisplayName(item);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, World player, List<String> list, ITooltipFlag b) {
		if (item.hasTagCompound()) {
			NBTTagCompound nbt = item.getTagCompound();
			int n = nbt.getIntArray("in").length, m = n + nbt.getIntArray("out").length;
			if (m > 0) {
				NBTTagList labels = nbt.getTagList("labels", NBT.TAG_STRING);
				StringBuilder sb = new StringBuilder("(");
				for (int i = 0; i < n; i++) {
					if (i > 0) sb.append(", ");
					sb.append("\u00a71").append(labels.getStringTagAt(i)).append("\u00a77");
				}
				sb.append(") -> (");
				for (int i = n; i < m; i++) {
					if (i > n) sb.append(", ");
					sb.append("\u00a72").append(labels.getStringTagAt(i)).append("\u00a77");
				}
				sb.append(")");
				list.add(sb.toString());
			}
			int[] stats = nbt.getIntArray("stats");
			if (stats.length < 4) stats = Arrays.copyOf(stats, 4);
			list.add(TooltipUtil.format("item.rs_ctr.processor.stats", stats[0], stats[1], stats[2], stats[3]));
			if (b.isAdvanced() && nbt.hasKey("IDm"))
				list.add("\u00a78" + new UUID(nbt.getLong("IDm"), nbt.getLong("IDl")).toString());
		}
		super.addInformation(item, player, list, b);
	}

	public void loadIngredients(ItemStack stack, ItemStack[] ingreds) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) Arrays.fill(ingreds, ItemStack.EMPTY);
		else ItemFluidUtil.loadItems(nbt.getTagList("ingr", NBT.TAG_COMPOUND), ingreds);
	}

	public void storeAll(ItemStack stack, ItemStack[] ingreds, int[] cmplx) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		nbt.setIntArray("stats", cmplx);
		nbt.setTag("ingr", ItemFluidUtil.saveItems(ingreds));
	}

	/**
	 * @param stack
	 * @param stats [0]:basic complexity, [1]:advanced complexity, [2]:memory bytes, [3]:size, [4]:energy per cycle, [5]:gain per tick, [6]:energy capacity
	 */
	public void loadStats(ItemStack stack, int[] stats) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) return;
		int[] arr = nbt.getIntArray("stats");
		System.arraycopy(arr, 0, stats, 0, Math.min(arr.length, stats.length));
	}

	public int maxInPorts(ItemStack stack) {
		return maxIn;
	}

	public int maxOutPorts(ItemStack stack) {
		return maxOut;
	}

}
