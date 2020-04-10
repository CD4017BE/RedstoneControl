package cd4017be.rs_ctr.tileentity;

import static cd4017be.api.rs_ctr.frame.IFrameOperator.*;
import static cd4017be.lib.util.TooltipUtil.format;
import static cd4017be.lib.util.TooltipUtil.translate;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import cd4017be.api.rs_ctr.com.SignalHandler;
import cd4017be.api.rs_ctr.frame.IFrameOperator;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent;
import cd4017be.api.rs_ctr.interact.IInteractiveComponent.ITESRenderComp;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import cd4017be.rs_ctr.IChunkLoader;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.Objects;
import cd4017be.rs_ctr.gui.BlockButton;
import cd4017be.rs_ctr.item.ItemChunkLoaderFuel;
import cd4017be.rs_ctr.render.FrameRenderer;
import cd4017be.rs_ctr.render.ISpecialRenderComp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class ChunkLoader extends WallMountGate
implements IFrameOperator, SignalHandler, IntConsumer, Supplier<String>, ISpecialRenderComp, ITESRenderComp, IChunkLoader, ITickableServerOnly {

	public static int RANGE = 64, MAX_CHUNKS, MAX_MINUTES = 2880;
	BlockButton button = new BlockButton(this, () -> null, this).setSize(0.5F, 0.25F);
	SignalHandler out = SignalHandler.NOP;
	int minutes;
	public int[] area = new int[] {0, -1, 0, -1, 1, -1};
	public byte missingFrames = -1;
	public byte mode;
	boolean showFrame = true;
	Ticket ticket;
	String owner = "";

	{
		ports = new MountedPort[] {
			new MountedPort(this, 0, SignalHandler.class, false).setName("port.rs_ctr.load").setLocation(.875, .125, 0, EnumFacing.NORTH),
			new MountedPort(this, 1, SignalHandler.class, true).setName("port.rs_ctr.time").setLocation(.125, .125, 0, EnumFacing.NORTH),
		};
	}

	@Override
	public SignalHandler getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		(out = callback instanceof SignalHandler ? (SignalHandler)callback : SignalHandler.NOP).updateSignal(minutes);
	}

	@Override
	protected void resetPin(int pin) {
		getPortCallback(pin).updateSignal(0);
	}

	@Override
	public void updateSignal(int value) {
		if (value < 0) value = 0;
		else if (value > 2) value = 2;
		if (mode == value) return;
		mode = (byte)value;
		turnOn();
		markDirty(SYNC);
	}

	@Override
	public void update() {
		if (ticket == null) return;
		long t = world.getTotalWorldTime();
		if ((t & 15) != 0 || t % 1200 != 0) return;
		
		int chunks = chunkCount();
		boolean all = ticket.getModData().getBoolean("all");
		boolean reduce = all && mode == 1 || missingFrames != 0 || (ticket.getChunkListDepth() > 0 && chunks > ticket.getChunkListDepth());
		if (!all || reduce) chunks = 1;
		else if (chunks > 2) chunks = 2;
		if (mode > 0 && minutes >= chunks) {
			if (reduce) forceChunks(false);
			out.updateSignal(minutes -= chunks);
		} else {
			ForgeChunkManager.releaseTicket(ticket);
			ticket = null;
		}
		markDirty(SYNC);
	}

	@Override
	public boolean setTicket(Ticket ticket) {
		if (this.ticket != null && this.ticket != ticket) ForgeChunkManager.releaseTicket(this.ticket);
		this.ticket = ticket;
		forceChunks(ticket.getModData().getBoolean("all"));
		return true;
	}

	private int chunkCount() {
		return ((area[0] + area[3] >> 4) - (area[0] - 1 >> 4) + 1)
			* ((area[2] + area[5] >> 4) - (area[2] - 1 >> 4) + 1);
	}

	private void forceChunks(boolean all) {
		if (ticket == null) {
			ticket = ForgeChunkManager.requestPlayerTicket(Main.instance, owner, world, Type.NORMAL);
			if (ticket == null) return;
			ticket.getModData().setLong("pos", pos.toLong());
		}
		ChunkPos me = getChunk().getPos();
		for (int cx = area[0] >> 4, cx1 = area[0] + area[3] >> 4; cx <= cx1; cx++)
			for (int cz = area[2] >> 4, cz1 = area[2] + area[5] >> 4; cz <= cz1; cz++) {
				ChunkPos pos = new ChunkPos(cx, cz);
				if (all)
					ForgeChunkManager.forceChunk(ticket, pos);
				else if (!pos.equals(me))
					ForgeChunkManager.unforceChunk(ticket, pos);
			}
		ForgeChunkManager.forceChunk(ticket, me);
		ticket.getModData().setBoolean("all", all);
	}

	private void turnOn() {
		if (mode <= 0) return;
		int chunks = chunkCount();
		boolean all = mode > 1 && missingFrames == 0 && chunks <= MAX_CHUNKS;
		if (!all) chunks = 1;
		else if (chunks > 2)chunks = 2;
		if (ticket != null) {
			if (!all || ticket.getModData().getBoolean("all")) return;
			chunks--;
		}
		if (minutes < chunks * 2) return;
		minutes -= chunks;
		forceChunks(all);
	}

	@Override
	public void onFrameBreak(BlockPos pos) {
		int i = 0, d;
		d = pos.getX() - area[0];
		if(d == area[3]) i |= 1;
		else if(d != -1) return;
		d = pos.getY() - area[1];
		if(d != 0) return;
		d = pos.getZ() - area[2];
		if(d == area[5]) i |= 4;
		else if(d != -1) return;
		missingFrames |= 5 << i;
		markDirty(SYNC);
	}

	@Override
	public String get() {
		if(Minecraft.getMinecraft().player.isSneaking())
			return translate("port.rs_ctr.show_sel" + (showFrame ? '1' : '0'));
		int dx = area[3] + 2, dz = area[5] + 2;
		int t = minutes, n = chunkCount();
		char state, status = n > MAX_CHUNKS ? '2'
			: missingFrames != 0 ? '1' : '0';
		if (mode > 1 && n > 1) {
			state = '2';
			t >>= 1;
		} else state = mode <= 0 ? '0' : '1';
		
		return format(
			"port.rs_ctr.area_cl" + status, dx, dz,
			n, MAX_CHUNKS, Integer.bitCount(missingFrames & 0xff) >> 1
		) + '\n' + format("port.rs_ctr.time_cl" + state, t / 60, t % 60);
	}

	@Override
	public void accept(int value) {
		if((value & BlockButton.A_HIT) != 0) {
			if (minutes <= 0) return;
			ItemStack stack = new ItemStack(Objects.cl_fuel);
			if ((minutes -= stack.getMaxDamage()) < 0) {
				stack.setItemDamage(-minutes);
				minutes = 0;
			}
			ItemFluidUtil.dropStack(stack, world, pos.offset(o.front));
			out.updateSignal(minutes);
			markDirty(SYNC);
		}
		if((value & BlockButton.A_SNEAKING) == 0) {
			if(
				area[3] <= 0 || area[4] <= 0 || area[5] <= 0
					|| (missingFrames = (byte)checkCorners(world, pos, area)) != 0
			) {
				unlinkCorners(world, pos, area, ~missingFrames);
				scanArea(world, pos, area, RANGE, getOrientation().front);
				area[1] = pos.getY();
				area[4] = 1;
				missingFrames = (byte)checkCorners(world, pos, area);
			}
			turnOn();
		} else showFrame = !showFrame;
		markDirty(SYNC);
	}

	@Override
	public boolean onActivated(
		EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z
	) {
		if (item.getItem() instanceof ItemChunkLoaderFuel) {
			if (minutes >= MAX_MINUTES) return false;
			if (!(world instanceof WorldServer)) return true;
			//It's allowed to automate this with FakePlayers, but not while the responsible person is offline. 
			if (((WorldServer)world).getMinecraftServer().getPlayerList().getPlayerByUUID(player.getUniqueID()) == null) return false;
			item = item.splitStack(1);
			if ((minutes += item.getMaxDamage() - item.getItemDamage()) > MAX_MINUTES) {
				item.setItemDamage(item.getMaxDamage() - minutes + MAX_MINUTES);
				ItemFluidUtil.dropStack(item, player);
				minutes = MAX_MINUTES;
			}
			owner = player.getName();
			turnOn();
			out.updateSignal(minutes);
			markDirty(SYNC);
			return true;
		} else return super.onActivated(player, hand, item, s, X, Y, Z);
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) nbt.setString("owner", owner);
		nbt.setInteger(
			"mode", mode == SAVE ? this.mode
				: (byte)(ticket == null ? 0 : ticket.getModData().getBoolean("all") ? 2 : 1)
		);
		writeArea(area, nbt, pos);
		nbt.setByte("frame", missingFrames);
		nbt.setBoolean("dsp", showFrame);
		nbt.setInteger("t", minutes);
		//not sure if forge syncs chunk loading config between server & client
		if (mode == CLIENT) nbt.setInteger("lim", ForgeChunkManager.getMaxChunkDepthFor(Main.ID));
		super.storeState(nbt, mode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		if (mode == SAVE) owner = nbt.getString("owner");
		this.mode = nbt.getByte("mode");
		readArea(area, nbt, pos);
		missingFrames = nbt.getByte("frame");
		showFrame = nbt.getBoolean("dsp");
		minutes = nbt.getInteger("t");
		if (mode == CLIENT && nbt.hasKey("lim", NBT.TAG_INT)) {
			MAX_CHUNKS = nbt.getInteger("lim");
			if (MAX_CHUNKS <= 0) MAX_CHUNKS = Integer.MAX_VALUE;
		}
		super.loadState(nbt, mode);
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		if (ticket != null) {
			ForgeChunkManager.releaseTicket(ticket);
			ticket = null;
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		turnOn();
	}

	@Override
	public void breakBlock() {
		super.breakBlock();
		@SuppressWarnings("deprecation")
		int cap = Objects.cl_fuel.getMaxDamage();
		ItemFluidUtil.dropStack(new ItemStack(Objects.cl_fuel, minutes / cap), world, pos);
		if ((minutes %= cap) > 0)
			ItemFluidUtil.dropStack(new ItemStack(Objects.cl_fuel, 1, cap - minutes), world, pos);
		minutes = 0;
		unlinkCorners(world, pos, area, ~missingFrames);
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		list.add(button);
	}

	@Override
	protected void orient(Orientation o) {
		if (area[1] < 0) {
			area[0] = pos.getX() + 1;
			area[1] = pos.getY();
			area[2] = pos.getZ() + 1;
		}
		button.setLocation(0.5, 0.625, 0, o);
		super.orient(o);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderSpecial(
		double x, double y, double z, float t, FontRenderer fr
	) {
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		int mf = missingFrames & 0xff;
		if(mf == 0) return;
		GlStateManager.glLineWidth(2.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.disableDepth();
		for(int i = 0; i < 8; i++, mf >>= 1) {
			if((mf & 1) == 0) continue;
			BlockPos p = getCorner(area, i).subtract(pos);
			double x1 = x + p.getX();
			double y1 = y + p.getY();
			double z1 = z + p.getZ();
			RenderGlobal.drawBoundingBox(
				x1, y1, z1, x1 + 1., y1 + 1., z1 + 1,
				1, 0, 0, 1
			);
		}
		GlStateManager.enableDepth();
		GlStateManager.enableTexture2D();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasFastRenderer() {
		return missingFrames == 0 && !HybridFastTESR.isAimedAt(this);
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		int d;
		return super.getMaxRenderDistanceSquared()
			+ (d = area[3]) * d
			+ (d = area[4]) * d
			+ (d = area[5]) * d;
	}

	@Override
	public void render(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		if(showFrame) {
			x += area[0] - pos.getX();
			y += area[1] - pos.getY();
			z += area[2] - pos.getZ();
			FrameRenderer.renderFrame(
				x - 1.03125, y - .03125, z - 1.03125,
				x + area[3] + 1.03125,
				y + area[4] + .03125,
				z + area[5] + 1.03125,
				buffer, missingFrames != 0 ? 0x7f0000ff : mode > 1 ? 0x7fff0000 : 0x7f00ff00
			);
		}
	}

	@Override
	public AxisAlignedBB getRenderBB(World world, BlockPos pos) {
		return new AxisAlignedBB(
			area[0] - 1, area[1], area[2] - 1,
			area[0] + area[3] + 1, area[1] + area[4], area[2] + area[5] + 1
		);
	}

	@Override
	public Object getState(int id) {
		return id == 0 ? (int)mode : minutes;
	}

}
