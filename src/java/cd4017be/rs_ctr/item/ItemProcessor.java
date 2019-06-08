package cd4017be.rs_ctr.item;

import java.util.List;
import java.util.UUID;

import cd4017be.lib.item.BaseItemBlock;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;


/**
 * @author CD4017BE
 *
 */
public class ItemProcessor extends BaseItemBlock {

	public ItemProcessor(Block id) {
		super(id);
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
			if (b.isAdvanced() && nbt.hasKey("IDm"))
				list.add("\u00a78" + new UUID(nbt.getLong("IDm"), nbt.getLong("IDl")).toString());
		}
		super.addInformation(item, player, list, b);
	}

}
