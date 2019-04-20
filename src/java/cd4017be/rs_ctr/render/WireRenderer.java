package cd4017be.rs_ctr.render;

import static java.lang.Float.floatToIntBits;

import cd4017be.lib.render.IModeledTESR;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

/**
 * @author CD4017BE
 *
 */
public class WireRenderer implements IModeledTESR {

	static final int TYPES = 2;
	public static WireRenderer instance;
	static final float WIDTH = 0.03125F, L_PLUG = 0.125F;

	private final IntArrayModel[] plugs = new IntArrayModel[6 * TYPES];

	public static void register() {
		if (instance == null)
			SpecialModelLoader.instance.tesrs.add(instance = new WireRenderer());
	}

	public float[] createLine(MountedSignalPort port, Vec3d line) {
		EnumFacing face = port.face;
		Vec3d l = new Vec3d(face.getDirectionVec());
		Vec3d a = line.crossProduct(l), b;
		if (a.lengthSquared() < 0.001) {
			Orientation o = Orientation.fromFacing(face);
			a = o.rotate(new Vec3d(WIDTH, 0.0, 0.0));
			b = o.rotate(new Vec3d(0.0, WIDTH, 0.0));
		} else {
			b = line.crossProduct(a);
			b = b.scale(WIDTH / b.lengthVector());
			a = a.scale(WIDTH / a.lengthVector());
		}
		TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("rs_ctr:blocks/rs_port0");
		Vec2f t0 = Util.getUV(tex, 0, 14), t1 = Util.getUV(tex, 16, 16);
		Vec3d p = l.scale(L_PLUG).add(port.pos);
		l = line.add(p);
		return new float[] {
			(float)(p.x - a.x), (float)(p.y - a.y), (float)(p.z - a.z), t0.x, t0.y,
			(float)(p.x + a.x), (float)(p.y + a.y), (float)(p.z + a.z), t0.x, t1.y,
			(float)(l.x + a.x), (float)(l.y + a.y), (float)(l.z + a.z), t1.x, t1.y,
			(float)(l.x - a.x), (float)(l.y - a.y), (float)(l.z - a.z), t1.x, t0.y,
			(float)(p.x - b.x), (float)(p.y - b.y), (float)(p.z - b.z), t0.x, t0.y,
			(float)(p.x + b.x), (float)(p.y + b.y), (float)(p.z + b.z), t0.x, t1.y,
			(float)(l.x + b.x), (float)(l.y + b.y), (float)(l.z + b.z), t1.x, t1.y,
			(float)(l.x - b.x), (float)(l.y - b.y), (float)(l.z - b.z), t1.x, t0.y,
		};
	}

	public void drawLine(BufferBuilder b, float[] v, float x, float y, float z, int l0, int l1, int c) {
		for (int i = 0, j = 0; i < v.length; j++)
			b.addVertexData(new int[] {
				floatToIntBits(v[i++] + x), floatToIntBits(v[i++] + y), floatToIntBits(v[i++] + z),
				c, floatToIntBits(v[i++]), floatToIntBits(v[i++]), (j & 2) == 0 ? l0 : l1
			});
	}

	public void drawPlug(BufferBuilder b, MountedSignalPort p, float x, float y, float z, int l0, int type) {
		IntArrayModel m = plugs[p.face.ordinal() + type * 6];
		m.setBrightness(l0);
		b.addVertexData(m.translated(x + (float)p.pos.x - 0.5F, y + (float)p.pos.y - 0.5F, z + (float)p.pos.z - 0.5F).vertexData);
	}

	@Override
	public void bakeModels(IResourceManager manager) {
		IntArrayModel main;
		for (int i = 0; i < TYPES; i++) {
			try {
				main = SpecialModelLoader.loadTESRModel(Main.ID, "plug.main(" + i + ")");
			} catch (Exception e) {
				Main.LOG.error("failed to load wire plug model", e);
				main = new IntArrayModel(0);
			}
			for (EnumFacing s : EnumFacing.VALUES) 
				plugs[s.ordinal() + i * 6] = main.rotated(Orientation.fromFacing(s));
		}
	}

}
