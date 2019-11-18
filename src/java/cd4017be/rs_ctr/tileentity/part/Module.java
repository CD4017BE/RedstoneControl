package cd4017be.rs_ctr.tileentity.part;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.item.ItemWrench;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 */
public abstract class Module implements IInteractiveComponent, INBTSerializable<NBTTagCompound> {

	public static final HashMap<String, Supplier<Module>> REGISTRY = new HashMap<>();
	public static Module get(String id) {
		Supplier<Module> loader = REGISTRY.get(id);
		return loader == null ? null : loader.get();
	}

	/**Warning on client side: asynchronous call of loadState() may set this to null at any time! */
	protected IPanel host;
	protected int idx;

	/**
	 * called when added to a panel
	 * @param ports add any connector ports here. Pin should be {@code idx * 2} or {@code idx * 2 + 1}
	 * @param idx index of this module
	 * @param panel to host this module
	 */
	public void init(List<MountedPort> ports, int idx, IPanel panel) {
		this.idx = idx;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("id", id());
		return nbt;
	}

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		IPanel host = this.host;
		if (host == null) return null;
		Orientation o = host.getOrientation();
		Pair<Vec3d, EnumFacing> res = IInteractiveComponent.rayTraceFlat(start, dir, o.Z.scale(.5).addVector(.5, .5, .5), o.back, 0.5F, 0.5F);
		if (res != null) {
			Vec3d pos = o.invRotate(res.getLeft().add(start).subtract(0.5, 0.5, 0.5));
			if ((getBounds() >> ((int)Math.floor(pos.x * 4D) + (int)Math.floor(pos.y * 4D) * 4 + 10) & 1) == 0)
				res = null;
		}
		return res;
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (stack.isEmpty() || !ItemWrench.WRENCHES.contains(stack.getItem().getRegistryName())) return false;
		if (hit) {
			stack = onRemove();
			if (!player.isCreative())
				ItemFluidUtil.dropStack(stack, player);
			host.remove(idx);
		} else if (player.isSneaking()) {
			stack.setTagCompound(serializeNBT());
			player.sendStatusMessage(new TextComponentTranslation("msg.rs_ctr.cfg_store"), true);
		} else if (stack.hasTagCompound()) {
			NBTTagCompound nbt = stack.getTagCompound();
			String id = nbt.getString("id");
			if (id.equals(id())) {
				loadCfg(nbt);
				host.markDirty(BaseTileEntity.REDRAW);
				player.sendStatusMessage(new TextComponentTranslation("msg.rs_ctr.cfg_load"), true);
			} else player.sendStatusMessage(new TextComponentTranslation("msg.rs_ctr.cfg_invalid"), true);
		} else GuiNetworkHandler.openBlockGui(player, host.pos(), idx);
		return true;
	}

	protected abstract void loadCfg(NBTTagCompound nbt);

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		return null;
	}

	/**
	 * render anything that requires calls to FontRenderer here.<br>
	 * Coordinates: (0,0) at top left (128,128) at bottom right
	 * @param fr the FontRenderer
	 */
	@SideOnly(Side.CLIENT)
	public void drawText(FontRenderer fr) {
	}

	/**
	 * perform setup for placement on a panel
	 * @param stack the Item used
	 * @param x place location from left
	 * @param y place location from bottom
	 */
	public abstract void onPlaced(ItemStack stack, float x, float y);

	/**@return the ID this is registered with */
	public abstract String id();

	/**@return 16-bit binary encoded bounding box defining which slots in a 4x4 raster are occupied */
	public abstract int getBounds();

	/**
	 * called when removed from panel
	 * @return the item to drop
	 */
	public abstract ItemStack onRemove();

	/**
	 * called when host loads
	 */
	public void onLoad(IPanel host) {
		this.host = host;
	}

	/**
	 * called when host unloads
	 */
	public void onUnload() {
		this.host = null;
	}

	public boolean loaded() {
		return host != null;
	}

	/**@return the input signal handler for {@link IPortProvider#getPortCallback(int)} */
	public abstract Object getPortCallback();

	/**@param callback the output signal handler from {@link IPortProvider#setPortCallback(int, Object)} */
	public abstract void setPortCallback(Object callback);

	/**called when input signal disconnected to reset it to its default value */
	public abstract void resetInput();

	/**
	 * encode all "fast changing" state for synchronization
	 * @param buf the packet to encode in
	 */
	public abstract void writeSync(PacketBuffer buf);

	/**
	 * decode all "fast changing" state from synchronization
	 * @param buf the packet to decode
	 */
	@SideOnly(Side.CLIENT)
	public abstract void readSync(PacketBuffer buf);

	/**
	 * @param player
	 * @return the Container for a configuration GUI
	 */
	public abstract Container getCfgContainer(EntityPlayer player);

	/**
	 * @param player
	 * @return the GuiScreen for a configuration GUI
	 */
	@SideOnly(Side.CLIENT)
	public abstract GuiScreen getCfgScreen(EntityPlayer player);

	/**
	 * implemented by TileEntities that can have panel modules attached
	 * @author cd4017be
	 */
	public interface IPanel extends IPortProvider {

		/**@return block orientation */
		Orientation getOrientation();

		/**@return block position */
		BlockPos pos();

		/**@return world object */
		World world();

		/**
		 * remove the given module
		 * @param idx module index
		 */
		void remove(int idx);

		/**
		 * try to add the given module to this panel
		 * @param m the module to add
		 * @return whether it could be added
		 */
		boolean add(Module m);

		/**
		 * schedule a "fast changing" state synchronization
		 */
		void updateDisplay();

		/**
		 * schedule a regular state synchronization
		 * @param mode {@link BaseTileEntity#SAVE}, {@link BaseTileEntity#SYNC} or {@link BaseTileEntity#REDRAW}
		 */
		void markDirty(int mode);

		/**
		 * @return combined light value in front of the panel
		 */
		@SideOnly(Side.CLIENT)
		int frontLight();
	}
}
