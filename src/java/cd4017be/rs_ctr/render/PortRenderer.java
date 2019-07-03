package cd4017be.rs_ctr.render;

import static java.lang.Float.floatToIntBits;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.api.interact.InteractiveDeviceRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static java.lang.Float.*;

/**
 * 
 * @author cd4017be
 */
@SideOnly(Side.CLIENT)
public class PortRenderer extends InteractiveDeviceRenderer {

	public static final PortRenderer PORT_RENDER = new PortRenderer();

	private final HashMap<String, IntArrayModel> cache = new HashMap<>();
	private final HashMap<String, BakedQuad[]> models = new HashMap<>();

	public void drawModel(BufferBuilder b, float x, float y, float z, Orientation o, int l, String model) {
		IntArrayModel m = cache.get(model);
		if (m == null) {
			BakedQuad[] bqs = models.get(model);
			if (bqs == null)
				m = new IntArrayModel(0);
			else {
				m = new IntArrayModel(bqs.length);
				for (int i = 0; i < bqs.length; i++)
					System.arraycopy(bqs[i].getVertexData(), 0, m.vertexData, i * 28, 28);
			}
			cache.put(model, m);
		}
		m.setBrightness(l);
		if (o != Orientation.N) m = m.rotated(o);
		b.addVertexData(m.translated(x - 0.5F, y - 0.5F, z - 0.5F).vertexData);
	}

	public void drawModel(List<BakedQuad> quads, float x, float y, float z, Orientation o, String model) {
		if (model != null && model.startsWith("rs_ctr:block/"))
			model = model.substring("rs_ctr:block/".length());
		BakedQuad[] bqs = models.get(model);
		if (bqs == null) return;
		ModelRotation r = o.getModelRotation();
		for (BakedQuad bq : bqs) {
			int[] vd = bq.getVertexData().clone();
			for (int i = 0; i < vd.length; i+=7) {
				Util.rotate(vd, i, r);
				vd[i + 0] = floatToIntBits(intBitsToFloat(vd[i + 0]) + x - 0.5F);
				vd[i + 1] = floatToIntBits(intBitsToFloat(vd[i + 1]) + y - 0.5F);
				vd[i + 2] = floatToIntBits(intBitsToFloat(vd[i + 2]) + z - 0.5F);
			}
			quads.add(new BakedQuad(vd, bq.getTintIndex(), o.rotate(bq.getFace()), bq.getSprite(), false, bq.getFormat()));
		}
	}

	@Override
	public void bake(VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
		cache.clear();
		for (ResourceLocation loc : dependencies) {
			IModel m = ModelLoaderRegistry.getModelOrLogError(loc, "missing");
			List<BakedQuad> quads = m.bake(m.getDefaultState(), format, textureGetter).getQuads(null, null, 0);
			String path = loc.getResourcePath();
			if (path.startsWith("block/")) path = path.substring("block/".length());
			if (loc.getResourceDomain() != Main.ID) path = loc.getResourceDomain() + ":" + path;
			models.put(path, quads.toArray(new BakedQuad[quads.size()]));
		}
	}

	public void register(String... models) {
		for (String s : models)
			dependencies.add(new ResourceLocation(Main.ID, "block/" + s));
	}

}
