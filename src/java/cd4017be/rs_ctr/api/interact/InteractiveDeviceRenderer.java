package cd4017be.rs_ctr.api.interact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import cd4017be.lib.render.HybridFastTESR;
import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.MultipartModel.IModelProvider;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.IBlockRenderComp;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent.ITESRenderComp;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
@SideOnly(Side.CLIENT)
public class InteractiveDeviceRenderer extends HybridFastTESR<TileEntity> implements IModelProvider {

	//Fast TESR

	@Override
	public void renderTileEntityFast(TileEntity te, double x, double y, double z, float t, int destroy, float alpha, BufferBuilder buffer) {
		Collection<ITESRenderComp> comps = ((IInteractiveDevice)te).getTESRComponents();
		if (comps.isEmpty()) return;
		World world = te.getWorld();
		BlockPos pos = te.getPos();
		int l = world.getCombinedLight(pos, 0);
		for (ITESRenderComp c : comps)
			c.render(world, pos, x, y, z, l, buffer);
	}

	//Normal TESR

	@Override
	protected void renderSpecialPart(TileEntity te, double x, double y, double z, float t, int destroy, float alpha) {
		Triple<IInteractiveComponent, Vec3d, EnumFacing> r = ((IInteractiveDevice)te).rayTrace(rendererDispatcher.entity, t);
		if (r == null) return;
		Pair<Vec3d, String> text = r.getLeft().getDisplayText(r.getMiddle());
		if (text == null) return;
		GlStateManager.pushMatrix();
		Orientation o = Orientation.fromFacing(r.getRight());
		if (o.ordinal() >= 4) {
			int i = Math.floorMod(Math.round(Minecraft.getMinecraft().player.rotationYaw / 90), 4);
			o = Orientation.values()[i + o.ordinal()];
		}
		Vec3d p = text.getKey();
		Util.moveAndOrientToBlock(x - 0.5 + p.x, y - 0.5 + p.y, z - 0.5 + p.z, o);
		float scale = 1F/128F;
		GlStateManager.scale(-scale, -scale, -scale);
		Util.renderToolTip(Minecraft.getMinecraft().fontRenderer, 0, -16, 0xffffff, 0x80000000, text.getValue().split("\n"));
		GlStateManager.popMatrix();
	}

	//Block Model rendering
	public final ArrayList<ResourceLocation> dependencies = new ArrayList<>();
	public final ArrayList<BiConsumer<VertexFormat, Function<ResourceLocation, TextureAtlasSprite>>> bakeCallbacks = new ArrayList<>();

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return dependencies;
	}

	@Override
	public void bake(VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
		for (BiConsumer<VertexFormat, Function<ResourceLocation, TextureAtlasSprite>> c : bakeCallbacks)
			c.accept(format, textureGetter);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void getQuads(List<BakedQuad> quads, Object val, BlockRenderLayer layer, IBlockState state, EnumFacing side, long rand) {
		if (layer != null && layer != BlockRenderLayer.CUTOUT || !(val instanceof Collection)) return;
		for (IBlockRenderComp brc : (Collection<IBlockRenderComp>)val)
			brc.render(quads);
	}

}
