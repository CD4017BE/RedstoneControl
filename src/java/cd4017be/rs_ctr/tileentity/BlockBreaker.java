package cd4017be.rs_ctr.tileentity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.item.ItemBlockBreaker;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/** @author CD4017BE */
public class BlockBreaker extends WallMountGate
implements ITickableServerOnly, SignalHandler, BlockHandler, ITilePlaceHarvest {

	public static float BASE_ENERGY = 1000, ENERGY_MULT = 4000, SPEED_MOD = 8, NO_TOOL_MULT = 0.5F;
	private static final int S_SUCCESS = 0, S_UNBREAKABLE = -1, S_FULLINV = 1, S_NOENERGY = 2;

	NonNullList<ItemStack> drops = NonNullList.create();
	BlockReference block;
	EnergyHandler energy = EnergyHandler.NOP;
	SignalHandler out;
	int clk, status, idle = 1000;
	boolean update;
	/** harvest mode: -2 -> no tool, -1 -> silk touch, n>=0 -> fortune level n */
	int fortune;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setName("port.rs_ctr.bi").setLocation(.75, .625, .875, EnumFacing.EAST),
			new MountedPort(this, 1, SignalHandler.class, false).setName("port.rs_ctr.clk").setLocation(.875, .25, .625, EnumFacing.UP),
			new MountedPort(this, 2, SignalHandler.class, true).setName("port.rs_ctr.status").setLocation(.875, .25, .125, EnumFacing.UP),
			new MountedPort(this, 3, EnergyHandler.class, true).setName("port.rs_ctr.energy_i").setLocation(.875, .25, .375, EnumFacing.UP),
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if(pin == 2) {
			out = callback instanceof SignalHandler ? (SignalHandler)callback : null;
			if(out != null)
				out.updateSignal(status);
		} else energy = callback instanceof EnergyHandler
			? (EnergyHandler)callback : EnergyHandler.NOP;
	}

	@Override
	protected void resetPin(int pin) {
		if(pin == 0)
			block = null;
	}

	@Override
	public void updateBlock(BlockReference ref) {
		block = ref;
	}

	@Override
	public void updateSignal(int value) {
		if(value == clk)
			return;
		clk = value;
		update = true;
	}

	@Override
	public void update() {
		idle++;
		if(!update)
			return;
		update = false;
		int status;
		if(!drops.isEmpty())
			status = clearDrops();
		else if(block != null && block.isLoaded())
			status = mineBlock(block);
		else status = S_UNBREAKABLE;
		if(status != this.status) {
			this.status = status;
			if(out != null)
				out.updateSignal(status);
		}
		if(status != S_NOENERGY)
			idle = 0;
	}

	private int clearDrops() {
		IItemHandler inv = Utils.neighborCapability(
			this, getOrientation().front, ITEM_HANDLER_CAPABILITY
		);
		if(inv == null)
			return S_FULLINV;
		for(int i = drops.size() - 1, j = i; i >= 0; i--) {
			ItemStack stack
			= ItemHandlerHelper.insertItemStacked(inv, drops.get(i), false);
			if(stack.getCount() > 0)
				drops.set(i, stack);
			else {
				stack = drops.remove(j--);
				if(j >= i)
					drops.set(i, stack);
			}
		}
		return drops.isEmpty() ? S_SUCCESS : S_FULLINV;
	}

	private int mineBlock(BlockReference ref) {
		IBlockState state = ref.getState();
		Material m = state.getMaterial();
		if(m == Material.AIR || m == Material.WATER || m == Material.LAVA)
			return S_UNBREAKABLE;
		float h = state.getBlockHardness(world, pos);
		if(h < 0)
			return S_UNBREAKABLE;
		int e = energyCost(h, m.isToolNotRequired());
		if(energy.changeEnergy(e, true) != e)
			return S_NOENERGY;
		World world = ref.world();
		Block block = state.getBlock();
		if(fortune >= 0 || (fortune == -1 ? !block.canSilkHarvest(world, ref.pos, state, null) : m.isToolNotRequired()))
			block.getDrops(drops, world, ref.pos, state, Math.max(0, fortune));
		else if (fortune == -1)
			drops.add(getSilkTouchDrop(state));
		world.setBlockToAir(ref.pos);
		energy.changeEnergy(e, false);
		return drops.isEmpty() ? S_SUCCESS : clearDrops();
	}

	private int energyCost(float h, boolean byHand) {
		h = h * ENERGY_MULT + BASE_ENERGY;
		h += h * SPEED_MOD / idle; // increased energy cost at high speed
		if (byHand) h *= NO_TOOL_MULT;
		return -(int)h;
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) {
			nbt.setInteger("clk", clk);
			nbt.setByte("out", (byte)status);
			nbt.setInteger("t", idle);
			nbt.setBoolean("update", update);
			if (block != null) nbt.setTag("ref", block.serializeNBT());
			else nbt.removeTag("ref");
			NBTTagList list = new NBTTagList();
			for (ItemStack stack : drops)
				if (!stack.isEmpty())
					list.appendTag(stack.writeToNBT(new NBTTagCompound()));
			nbt.setTag("drop", list);
		}
		if (mode <= CLIENT)
			nbt.setByte("type", (byte)fortune);
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) {
			clk = nbt.getInteger("clk");
			status = nbt.getByte("out");
			idle = nbt.getInteger("t");
			block = nbt.hasKey("ref", NBT.TAG_COMPOUND) ?
				new BlockReference(nbt.getCompoundTag("ref")) : null;
			NBTTagList list = nbt.getTagList("drop", NBT.TAG_COMPOUND);
			drops.clear();
			for (int i = 0; i < list.tagCount(); i++)
				drops.add(new ItemStack(list.getCompoundTagAt(i)));
		}
		if (mode <= CLIENT)
			fortune = nbt.getByte("type");
		super.loadState(nbt, mode);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (item.getItem() instanceof ItemBlockBreaker) {
			Integer f;
			Map<Enchantment, Integer> ench = EnchantmentHelper.getEnchantments(item);
			if ((f = ench.get(Enchantments.FORTUNE)) != null) fortune = f;
			else if ((f = ench.get(Enchantments.SILK_TOUCH)) != null) fortune = -1;
			else fortune = 0;
		} else fortune = -2;
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int f) {
		getBlockState();
		ItemStack stack = new ItemStack(blockType);
		if (fortune == -1)
			EnchantmentHelper.setEnchantments(Collections.singletonMap(Enchantments.SILK_TOUCH, 1), stack);
		else if (fortune > 0)
			EnchantmentHelper.setEnchantments(Collections.singletonMap(Enchantments.FORTUNE, fortune), stack);
		return Arrays.asList(stack);
	}

	private static ItemStack getSilkTouchDrop(IBlockState state) {
		try {
			return (ItemStack)getSTD.invoke(state.getBlock(), state);
		} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return ItemStack.EMPTY;
		}
	}

	private static final Method getSTD;
	static {
		Method m;
		try {
			m = Block.class.getDeclaredMethod("func_180643_i", IBlockState.class);
		} catch (NoSuchMethodException e) {
			try {
				m = Block.class.getDeclaredMethod("getSilkTouchDrop", IBlockState.class);
			} catch(NoSuchMethodException e1) {
				throw new RuntimeException(e1);
			}
		}
		m.setAccessible(true);
		getSTD = m;
	}

}
