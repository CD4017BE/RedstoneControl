package cd4017be.rs_ctr.tileentity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/** @author CD4017BE */
public class BlockBreaker extends WallMountGate
implements ITickableServerOnly, SignalHandler, BlockHandler {

	public static float BASE_ENERGY = 100, ENERGY_MULT = 50, SPEED_MOD = 8;
	private static final int S_SUCCESS = 0, S_UNBREAKABLE = -1, S_NOENERGY = 1, S_FULLINV = 2;

	NonNullList<ItemStack> drops = NonNullList.create();
	BlockReference block;
	EnergyHandler energy = EnergyHandler.NOP;
	SignalHandler out;
	int clk, status, idle = 1000;
	boolean update;
	/** harvest mode: -2 -> no drops, -1 -> silk touch, n>=0 -> fortune level n */
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
		if(state.getMaterial() == Material.AIR)
			return S_UNBREAKABLE;
		float h = state.getBlockHardness(world, pos);
		if(h < 0)
			return S_UNBREAKABLE;
		int e = energyCost(h);
		if(energy.changeEnergy(e, true) != e)
			return S_NOENERGY;
		World world = ref.world();
		Block block = state.getBlock();
		if(fortune >= 0)
			block.getDrops(drops, world, ref.pos, state, fortune < 0 ? 0 : fortune);
		else if(fortune == -1 && block.canSilkHarvest(world, ref.pos, state, null))
			drops.add(getSilkTouchDrop(state));
		world.setBlockToAir(ref.pos);
		energy.changeEnergy(e, false);
		return drops.isEmpty() ? S_SUCCESS : clearDrops();
	}

	private int energyCost(float h) {
		h = h * ENERGY_MULT + BASE_ENERGY;
		h += h * SPEED_MOD / idle; // increased energy cost at high speed
		return -(int)h;
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		// TODO Auto-generated method stub
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		// TODO Auto-generated method stub
		super.loadState(nbt, mode);
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
