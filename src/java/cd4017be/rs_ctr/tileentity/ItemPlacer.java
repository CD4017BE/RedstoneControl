package cd4017be.rs_ctr.tileentity;

import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.com.EnergyHandler;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.com.BlockReference.BlockHandler;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.HidableSlot;
import cd4017be.lib.Gui.SlotArmor;
import cd4017be.lib.Gui.SlotOffhand;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.SaferFakePlayer;
import cd4017be.rs_ctr.gui.GuiItemPlacer;
import cd4017be.rs_ctr.util.FullHotbarInventory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public class ItemPlacer extends WallMountGate
implements ITickableServerOnly, SignalHandler, BlockHandler, ITilePlaceHarvest, IGuiHandlerTile, IStateInteractionHandler {

	public static float BASE_ENERGY = 100, SPEED_MOD = 8;
	private static final int S_SUCCESS = 0, S_PASS = -1, S_FAIL = 1, S_NOENERGY = 2;

	private GameProfile gp = new GameProfile(new UUID(0, 0), "dummyPlayer");
	private FakePlayer player;
	private InventoryPlayer inv = new FullHotbarInventory(null);
	private IItemHandler mainhand = new LinkedInventory(
		1, 64, (i)-> inv.mainInventory.get(inv.currentItem),
		(stack, i)-> inv.mainInventory.set(inv.currentItem, stack)
	), offhand = new LinkedInventory(
		1, 64, (i)-> inv.offHandInventory.get(0),
		(stack, i)-> inv.offHandInventory.set(0, stack)
	), maininv = new LinkedInventory(
		inv.mainInventory.size(), 64,
		inv.mainInventory::get,
		(stack, i)-> inv.mainInventory.set(i, stack)
	), armorinv = new PlayerArmorInvWrapper(inv);
	private boolean creative;
	private BlockReference block;
	private EnergyHandler energy = EnergyHandler.NOP;
	private SignalHandler out;
	private int clk, status, idle = 1000;
	/**bit[0,1]: yaw, bit[2,3]: pitch, bit[4]: sneak, bit[5]: creative, bit[8-11]: pixelX, bit[12-15]: pixelY, bit[16-22]: hotbarSlot */
	public int aim;
	private boolean update;

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, BlockHandler.class, false).setName("port.rs_ctr.bi").setLocation(.75, .625, .875, EnumFacing.EAST),
			new MountedPort(this, 1, SignalHandler.class, false).setName("port.rs_ctr.place").setLocation(.875, .25, .875, EnumFacing.UP),
			new MountedPort(this, 2, SignalHandler.class, false).setName("port.rs_ctr.clk").setLocation(.875, .25, .625, EnumFacing.UP),
			new MountedPort(this, 3, SignalHandler.class, true).setName("port.rs_ctr.status").setLocation(.875, .25, .125, EnumFacing.UP),
			new MountedPort(this, 4, EnergyHandler.class, true).setName("port.rs_ctr.energy_i").setLocation(.875, .25, .375, EnumFacing.UP),
		};
	}

	@Override
	public Object getPortCallback(int pin) {
		return pin == 1 ? (SignalHandler)this::updateAim : this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if(pin == 3) {
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
		else if (pin == 1)
			updateAim(0);
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

	public void updateAim(int value) {
		value &= creative ? 0x3fff3f : 0x3fff1f;
		if (value == aim) return;
		aim = value;
		if ((player.inventory.currentItem = aim >> 16) >= 36)
			player.inventory.currentItem = 0;
		markDirty(SAVE);
	}

	@Override
	public void update() {
		idle++;
		if(!update) return;
		update = false;
		int status;
		if(block != null && block.isLoaded())
			status = placeBlock(block);
		else status = S_PASS;
		if(status != this.status) {
			this.status = status;
			if(out != null)
				out.updateSignal(status);
		}
		if(status != S_NOENERGY)
			idle = 0;
	}

	private void initializePlayer() {
		if (!(world instanceof WorldServer)) return;
		player = new SaferFakePlayer((WorldServer)world, gp);
		player.capabilities.allowFlying = true;
		player.capabilities.disableDamage = true;
		player.capabilities.isFlying = true;
		player.inventory = inv;
		inv.player = player;
	}

	private int placeBlock(BlockReference ref) {
		int e = energyCost();
		if(energy.changeEnergy(e, true) != e)
			return S_NOENERGY;
		if (player == null) initializePlayer();
		RayTraceResult res = setupInteraction(player, ref, aim);
		BlockPos pos = res.getBlockPos();
		float X = (float)res.hitVec.x - pos.getX();
		float Y = (float)res.hitVec.y - pos.getY();
		float Z = (float)res.hitVec.z - pos.getZ();
		EnumActionResult ar = null;
		for (EnumHand hand : EnumHand.values()) {
			ItemStack stack = player.getHeldItem(hand);
			if (ar != null && stack.isEmpty()) break;
			ar = player.interactionManager.processRightClickBlock(
				player, player.world, stack,
				hand, pos, res.sideHit, X, Y, Z
			);
			if (ar != EnumActionResult.PASS) break;
		}
		energy.changeEnergy(e, false);
		return ar == EnumActionResult.SUCCESS ? S_SUCCESS :
			ar == EnumActionResult.FAIL ? S_FAIL : S_PASS;
	}

	private int energyCost() {
		float h = BASE_ENERGY;
		h += h * SPEED_MOD / idle; // increased energy cost at high speed
		return -(int)h;
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) {
			nbt.setInteger("clk", clk);
			nbt.setByte("out", (byte)status);
			nbt.setInteger("aim", aim);
			nbt.setInteger("t", idle);
			nbt.setBoolean("update", update);
			if (block != null) nbt.setTag("ref", block.serializeNBT());
			else nbt.removeTag("ref");
			nbt.setUniqueId("FPuuid", gp.getId());
			nbt.setString("FPname", gp.getName());
			nbt.setBoolean("creative", creative);
			nbt.setTag("FPinv", inv.writeToNBT(new NBTTagList()));
		}
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) {
			clk = nbt.getInteger("clk");
			status = nbt.getByte("out");
			aim = nbt.getInteger("aim");
			idle = nbt.getInteger("t");
			block = nbt.hasKey("ref", NBT.TAG_COMPOUND) ?
				new BlockReference(nbt.getCompoundTag("ref")) : null;
			gp = new GameProfile(nbt.getUniqueId("FPuuid"), nbt.getString("FPname"));
			creative = nbt.getBoolean("creative");
			inv.readFromNBT(nbt.getTagList("FPinv", NBT.TAG_COMPOUND));
			if ((player.inventory.currentItem = aim >> 16) >= 36)
				player.inventory.currentItem = 0;
		}
		super.loadState(nbt, mode);
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return null;
		if (player == null) initializePlayer();
		Orientation o = getOrientation();
		if (facing == o.front)
			return (T)mainhand;
		if (facing == o.back)
			return (T)offhand;
		if (facing.getAxis() == Axis.Y)
			return (T)maininv;
		return (T)armorinv;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			gp = player.getGameProfile();
			creative = player.isCreative();
		}
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		return makeDefaultDrops();
	}

	private static RayTraceResult setupInteraction(FakePlayer player, BlockReference block, int aim) {
		player.setSneaking((aim & 16) != 0);
		player.capabilities.isCreativeMode = (aim & 32) != 0;
		//create aim vector
		Vec3d vec = new Vec3d(
			.46875 - (aim >> 8 & 15) * .0625,
			(aim >> 12 & 15) * .0625 - .46875,
			-2.0
		);
		switch(block.face) {
		case WEST: aim = aim + 3 & 3 | aim & 12; break;
		case SOUTH: aim = aim & 15 ^ 2; break;
		case EAST: aim = aim + 1 & 3 | aim & 12; break;
		default: aim &= 15;
		}
		Orientation o = Orientation.values()[aim];
		BlockPos pos = block.pos;
		vec = o.rotate(vec).addVector(
			pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5
		);
		//position & orient player
		player.dimension = block.dim;
		player.world = block.world();
		player.setLocationAndAngles(
			vec.x, vec.y - player.getEyeHeight(), vec.z,
			(aim & 3) * 90, (((aim >> 2) + 1 & 3) - 1) * -90
		);
		player.prevRotationYawHead = player.rotationYawHead = player.rotationYaw;
		//ray trace
		Vec3d dir = vec.addVector(
			o.back.getFrontOffsetX() * 2.5,
			o.back.getFrontOffsetY() * 2.5,
			o.back.getFrontOffsetZ() * 2.5
		);
		RayTraceResult res = block.getState().collisionRayTrace(
			player.world, pos, vec, dir
		);
		if (res != null) return res;
		return new RayTraceResult(dir, o.front, pos);
	}

	private static final StateSynchronizer.Builder ssb
	= StateSynchronizer.builder().addFix(4);

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		AdvancedContainer cont = new AdvancedContainer(this, ssb.build(world.isRemote), player);
		int x = 26, y = 26;
		for (int i = 0; i < 3; i++) 
			for (int j = 0; j < 9; j++)
				cont.addItemSlot(new HidableSlot(inv, i * 9 + j + 9, x + j * 18, y + i * 18), false);
		for (int i = 0; i < 9; i++)
			cont.addItemSlot(new HidableSlot(inv, i, x + i * 18, y + 58), false);
		cont.addItemSlot(new SlotOffhand(inv, 40, x - 18, y + 58), false);
		for (int i = 0; i < 4; i++)
			cont.addItemSlot(new SlotArmor(inv, i + 36, x - 18, y - i * 18 + 36, EntityEquipmentSlot.values()[i + 2]), false);
		cont.addPlayerInventory(26, 124, true, false);
		return cont;
	}


	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.putAll(aim).endFixed();
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		aim = state.get(aim);
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return canPlayerAccessUI(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiItemPlacer getGuiScreen(EntityPlayer player, int id) {
		return new GuiItemPlacer(this, player);
	}

}
