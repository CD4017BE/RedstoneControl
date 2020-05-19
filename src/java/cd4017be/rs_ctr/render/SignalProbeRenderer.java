package cd4017be.rs_ctr.render;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.vecmath.Matrix4f;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.gui.ChatFormatting;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.lib.render.IHardCodedModel;
import cd4017be.lib.render.SpecialModelLoader;
import static cd4017be.lib.util.TooltipUtil.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import static net.minecraft.client.renderer.GlStateManager.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
@SideOnly(Side.CLIENT)
public class SignalProbeRenderer extends TileEntityItemStackRenderer implements IHardCodedModel, IModel, IBakedModel {

	private final ResourceLocation parentName;
	private IBakedModel parent;
	private FontRenderer fr;
	int scroll;

	public SignalProbeRenderer(Item item) {
		ResourceLocation loc = item.getRegistryName();
		this.parentName = new ResourceLocation(loc.getResourceDomain(), "item/" + loc.getResourcePath() + "_0");
		SpecialModelLoader.registerItemModel(item, this);
	}

	@Override
	public void renderByItem(ItemStack stack, float t) {
		Minecraft mc = Minecraft.getMinecraft();
		translate(0.5F, 0.5F, 0.5F);
		mc.getRenderItem().renderItem(stack, parent);
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) return;
		translate(-0.1875F, 0.4375F, 0.032F);
		scale(1D/256D, -1D/256D, 1);
		disableLighting();
		renderDisplay(nbt, mc.objectMouseOver, mc.world, mc.player, t);
	}

	private void renderDisplay(NBTTagCompound data, RayTraceResult rtr, World world, EntityPlayer player, float t) {
		if (rtr.typeOfHit != Type.BLOCK) return;
		BlockPos pos = rtr.getBlockPos();
		IPortProvider pp; {
			IBlockState state = world.getBlockState(pos);
			ItemStack stack = state.getBlock().getPickBlock(state, rtr, world, pos, player);
			String s = ChatFormatting.stripFormatting(stack.getDisplayName());
			if (pos.hashCode() != data.getInteger("tgt")) {
				print("\u00a7l\u00a7k" + s, 4, 4, 152, 0xffffff00);
				return;
			}
			print("\u00a7l" + s, 4, 4, 152, 0xffffff00);
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IPortProvider) pp = (IPortProvider)te;
			else {
				fr.drawString(translate("sp.rs_ctr.invalid"), 4, 20, -1);
				return;
			}
		}
		Vec3d p = player.getPositionEyes(t).subtract(pos.getX(), pos.getY(), pos.getZ()),
			d = player.getLook(t).scale(16);
		NBTTagList list = data.getTagList("ports", NBT.TAG_COMPOUND);
		if (list.tagCount() <= 9) scroll = 0;
		int sel = -1, y = 20;
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt = list.getCompoundTagAt(i);
			int pin = nbt.getInteger("id");
			Port port;
			try {
				port = pp.getPort(pin);
				if (port == null) continue;
			} catch(Exception e) {continue;}
			if (port instanceof MountedPort) {
				Pair<Vec3d, EnumFacing> hit = ((MountedPort)port).rayTrace(p, d);
				if (hit != null) {
					d = hit.getLeft();
					sel = i;
				}
			}
			if (i >= scroll && i < scroll + 9) {
				printPortName(port, 4, y, 152);
				printValue(nbt, 4, y + 9, 152);
				y += 21;
			}
		}
		if (sel >= 0) {
			if (sel < scroll) scroll = sel;
			else if (sel > scroll + 8) scroll = sel - 8;
			sel -= scroll;
			sel *= 21;
			bindTexture(0);
			color(1.0F, 0.8F, 0.0F);
			glBegin(GL11.GL_LINE_STRIP);
			glVertex3f(2, 18 + sel, 0);
			glVertex3f(158, 18 + sel, 0);
			glVertex3f(158, 38 + sel, 0);
			glVertex3f(2, 38 + sel, 0);
			glVertex3f(2, 18 + sel, 0);
			glEnd();
		}
	}

	private void printPortName(Port p, int x, int y, int w) {
		StringBuilder sb = new StringBuilder();
		int i = p.getLink();
		if (i != 0) {
			String s;
			Connector c;
			if (
				p instanceof MountedPort
				&& (c = ((MountedPort)p).getConnector()) instanceof ITagableConnector
				&& (s = ((ITagableConnector)c).getTag()) != null
			) sb.append("\u00a7e\u00a7o" + s + "\u00a7r");
			else sb.append(format("sp.rs_ctr.id", i));
		} else if (p instanceof MountedPort && ((MountedPort)p).getConnector() != null)
			sb.append(translate("sp.rs_ctr.plug"));
		else sb.append(translate("sp.rs_ctr.none"));
		sb.append(translate(p.isMaster ? "sp.rs_ctr.o" : "sp.rs_ctr.i"));
		if (p instanceof MountedPort) sb.append(translate(((MountedPort)p).name));
		else sb.append("\u00a7lPin ").append(p.pin);
		print(sb.toString(), x, y, w, -1);
	}

	private void printValue(NBTTagCompound nbt, int x, int y, int w) {
		if (nbt.hasKey("val"))
			switch(nbt.getByte("type")) {
			case 0:
				print(format("sp.rs_ctr.num", nbt.getInteger("val")), x, y, w, 0xffff8080);
				break;
			case 1:
				NBTTagCompound tag = nbt.getCompoundTag("val");
				String s;
				if (tag.hasNoTags()) s = translate("sp.rs_ctr.null");
				else {
					BlockReference ref = new BlockReference(tag);
					s = format("sp.rs_ctr.block", ref.pos.getX(), ref.pos.getY(), ref.pos.getZ(), ref.dim, ref.face);
				}
				print(s, x, y, w, 0xff40c0c0);
				break;
			}
		else print(translate("sp.rs_ctr.unknown"), x, y, w, 0xff808080);
	}

	private void print(String s, int x, int y, int w, int c) {
		if (fr.getStringWidth(s) > w)
			s = fr.trimStringToWidth(s, w - fr.getStringWidth("...")) + "...";
		fr.drawString(s, x, y, c);
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.of(parentName);
	}

	@Override
	public IBakedModel bake(
		IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter
	) {
		IModel m = ModelLoaderRegistry.getModelOrLogError(parentName, "missing base model");
		this.parent = m.bake(state, format, bakedTextureGetter);
		this.fr = Minecraft.getMinecraft().fontRenderer;
		return this;
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		return parent.getQuads(state, side, rand);
	}

	@Override
	public boolean isAmbientOcclusion() {
		return parent.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return parent.isGui3d();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return true;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return parent.getParticleTexture();
	}

	@Override
	public ItemOverrideList getOverrides() {
		return parent.getOverrides();
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType transform) {
		Pair<? extends IBakedModel, Matrix4f> p = parent.handlePerspective(transform);
		if(transform != TransformType.FIRST_PERSON_RIGHT_HAND && transform != TransformType.THIRD_PERSON_RIGHT_HAND)
			return p;
		return Pair.of(this, p.getRight());
	}

	@Override
	public void onReload() {
		parent = null;
	}

}
