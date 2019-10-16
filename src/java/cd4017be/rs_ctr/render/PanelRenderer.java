package cd4017be.rs_ctr.render;

import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.util.Orientation;
import static cd4017be.rs_ctr.ClientProxy.t_dial;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import static java.lang.Float.floatToIntBits;

import java.util.Arrays;
import java.util.List;

/**
 * @author CD4017BE
 *
 */
@SideOnly(Side.CLIENT)
public class PanelRenderer {

	static IntArrayModel create(double rad0, double rad1, double angle0, double angle1, int div) {
		int[] data = new int[28 * div];
		int[] p = new int[4];
		int v0i = floatToIntBits(14), v1i = floatToIntBits(16);
		vertex(p, rad0, rad1, angle0);
		for (int i = 0; i < div; i++) {
			int j = i * 28;
			data[j] = p[2]; data[j+1] = p[3]; data[j+5] = v1i;
			data[j+7] = p[0]; data[j+8] = p[1]; data[j+12] = v0i;
			vertex(p, rad0, rad1, angle0 + (angle1 - angle0) * (double)(i+1) / (double)div);
			data[j+14] = p[0]; data[j+15] = p[1]; data[j+19] = v0i;
			data[j+21] = p[2]; data[j+22] = p[3]; data[j+26] = v1i;
		}
		return new IntArrayModel(data);
	}

	static void vertex(int[] v, double rad0, double rad1, double angle) {
		double sin = Math.sin(angle), cos = Math.cos(angle);
		v[0] = floatToIntBits((float)(rad0 * sin));
		v[1] = floatToIntBits((float)(rad0 * cos));
		v[2] = floatToIntBits((float)(rad1 * sin));
		v[3] = floatToIntBits((float)(rad1 * cos));
	}

	static void drawPin(List<BakedQuad> quads, float x, float y, float z, float d, EnumFacing side) {
		int u0 = floatToIntBits(t_dial.getInterpolatedU(0)), u1 = floatToIntBits(t_dial.getInterpolatedU(.5)), u2 = floatToIntBits(t_dial.getInterpolatedU(1)),
			v0 = floatToIntBits(t_dial.getInterpolatedV(13)), v1 = floatToIntBits(t_dial.getInterpolatedV(13.5)), v2 = floatToIntBits(t_dial.getInterpolatedV(14));
		int z0 = floatToIntBits(z), z1 = floatToIntBits(z + 0.0625F);
		quads.add(new BakedQuad(new int[] {
				floatToIntBits(x - d), floatToIntBits(y + d), z0, -1, u0, v0, 0,
				floatToIntBits(x - d), floatToIntBits(y - d), z0, -1, u0, v2, 0,
				floatToIntBits(x    ), floatToIntBits(y    ), z1, -1, u1, v1, 0,
				floatToIntBits(x + d), floatToIntBits(y + d), z0, -1, u2, v0, 0
			}, -1, side, t_dial, true, DefaultVertexFormats.BLOCK));
		quads.add(new BakedQuad(new int[] {
				floatToIntBits(x    ), floatToIntBits(y    ), z1, -1, u1, v1, 0,
				floatToIntBits(x - d), floatToIntBits(y - d), z0, -1, u0, v2, 0,
				floatToIntBits(x + d), floatToIntBits(y - d), z0, -1, u2, v2, 0,
				floatToIntBits(x + d), floatToIntBits(y + d), z0, -1, u2, v0, 0
			}, -1, side, t_dial, true, DefaultVertexFormats.BLOCK));
	}

	/**
	 * @param d 0x00 - 0x09 -> .0 - .9, 0x10 - 0x19 -> 0 - 9, 0x20 - 0x29 -> 00 - 90, 0x30 - 0x39 -> 000 - 900
	 */
	static BakedQuad digit(float x, float y, float z, float h, int d, EnumFacing side, int color) {
		float f, w;
		if (d < 0) {
			f = 15.25F;
			w = .75F;
			d = 0;
		} else if (d < 16) {
			f = 12.75F;
			w = 1.25F;
		} else {
			f = 13.25F;
			w = (float)(d >> 4) - .25F;
		}
		int zi = floatToIntBits(z);
		int u0 = floatToIntBits(t_dial.getInterpolatedU(f)), u1 = floatToIntBits(t_dial.getInterpolatedU(f + w));
		f = (float)(d & 15) * 1.25F;
		int v0 = floatToIntBits(t_dial.getInterpolatedV(f)), v1 = floatToIntBits(t_dial.getInterpolatedV(f + 1.25F));
		w *= h / 1.25F; h /= 2;
		int[] data = new int[] {
			floatToIntBits(x), floatToIntBits(y + h), zi, color, u0, v0, 0,
			floatToIntBits(x), floatToIntBits(y - h), zi, color, u0, v1, 0,
			floatToIntBits(x + w), floatToIntBits(y - h), zi, color, u1, v1, 0,
			floatToIntBits(x + w), floatToIntBits(y + h), zi, color, u1, v0, 0
		};
		return new BakedQuad(data, -1, side, t_dial, true, DefaultVertexFormats.BLOCK);
	}

	static float len(int i, int exp) {
		if (i < 0) return len(-i, exp) + 0.6F;
		if (i == 0) return 0.6F;
		while(exp < 0 && i % 10 == 0) {
			exp++; i /= 10;
		}
		float l = exp < 0 ? .4F : (float)exp * .8F;
		for (; i >= 10; i /= 10) l += .8F;
		return l + .6F;
	}

	static float drawNumber(List<BakedQuad> quads, float x, float y, float z, float h, int n, int exp, int color, EnumFacing side) {
		if (n < 10) {
			quads.add(digit(x, y, z, h, n | (exp + 1) << 4, side, color));
			return (exp < 0 ? .4F : (float)exp * .8F) + .8F;
		} else {
			float w = drawNumber(quads, x, y, z, h, n / 10, Math.min(exp + 1, 0), color, side);
			return w + drawNumber(quads, x + w * h, y, z, h, n % 10, exp < -1 ? 0 : exp, color, side);
		}
	}

	public enum Layout {
		QUARTER(0.625, 0.75, -90, 0, 10, 10, new Vec3d(0.875, 0.125, 1.003)),
		CIRCLE(0.3125, 0.375, -150, 150, 20, 32, new Vec3d(0.5, 0.4375, 1.003));
		
		public final double angle0, angle1, rad0, rad1;
		public final int n_div;
		public final int precision;
		public final Vec3d offset;
		public final IntArrayModel model;

		private Layout(double rad0, double rad1, double angle0, double angle1, int precision, int n_div, Vec3d offset) {
			this.rad0 = rad0;
			this.rad1 = rad1;
			this.angle0 = Math.toRadians(angle0);
			this.angle1 = Math.toRadians(angle1);
			this.precision = precision;
			this.n_div = n_div;
			this.offset = offset;
			this.model = create(rad0, rad1, this.angle0, this.angle1, n_div).translated((float)offset.x, (float)offset.y, (float)offset.z);
		}

		public static Layout of(int i) {
			return values()[i & 1];
		}

		public IntArrayModel getPointer(double f, int light) {
			f = angle0 + (angle1 - angle0) * MathHelper.clamp(f, -0.04, 1.04);
			float fy = (float)(Math.cos(f) * rad1), fx = (float)(Math.sin(f) * rad1);
			float px = (float)offset.x, py = (float)offset.y;
			int pz = floatToIntBits((float)offset.z + 0.03125F);
			TextureAtlasSprite tex = t_dial;
			int u0 = floatToIntBits(tex.getMinU()), u1 = floatToIntBits(tex.getInterpolatedU(4)), u2 = floatToIntBits(tex.getInterpolatedU(12)),
				v0 = floatToIntBits(tex.getMinV()), v1 = floatToIntBits(tex.getInterpolatedV(4)), v2 = floatToIntBits(tex.getInterpolatedV(12));
			return new IntArrayModel(new int[] {
					floatToIntBits(px +       fx), floatToIntBits(py +       fy), pz, -1, u0, v0, light,
					floatToIntBits(px + .5F * fy), floatToIntBits(py - .5F * fx), pz, -1, u2, v1, light,
					floatToIntBits(px - .5F * fx), floatToIntBits(py - .5F * fy), pz, -1, u2, v2, light,
					floatToIntBits(px - .5F * fy), floatToIntBits(py + .5F * fx), pz, -1, u1, v2, light,
			}, -1, light);
		}

		public void drawScale(List<BakedQuad> quads, Orientation o, int min, int max, int exp, int color) {
			EnumFacing side = o.back;
			int scale = 1;
			float mag = Math.abs((long)max - (long)min);
			while(mag > precision) {
				mag /= 10;
				scale *= 10;
				exp++;
			}
			float f = (float)min / (float)scale, df = (float)((long)max - (long)min) / (float)scale;
			//scale ring
			int[] data = model.vertexData;
			float df1 = df / n_div, f1 = f;
			for (int i = 4; i < data.length; i += 7, f1 += df1) {
				float u = f1 % 1;
				if (u < 0) u++;
				if (df1 < 0) u++;
				data[i] = data[i += 7] = floatToIntBits(u * 7.5F + 0.125F);
				data[i += 7] = data[i += 7] = floatToIntBits((u + df1) * 7.5F + 0.125F);
			}
			model.setColor(color);
			data = model.withTexture(t_dial).vertexData;
			int q = quads.size();
			for (int i = 0; i < data.length;)
				quads.add(new BakedQuad(Arrays.copyOfRange(data, i, i += 28), -1, side, t_dial, true, DefaultVertexFormats.BLOCK));
			//scale numbers
			if (Math.abs(f) <= 1000) {
				int de = 0;
				for (float f_ = Math.max(Math.abs(f), Math.abs(f + df)); f_ >= 10; f_ /= 10) de++;
				exp = Math.floorMod(exp + de, 3) - de;
				int m = 1;
				//if (exp == 2 && mag >= 10) exp = -1;
				if (mag > precision >> 1) {
					m = 2;
				} else if (mag <= precision >> 2) {
					m = 5;
					f *= 10;
					df *= 10;
					exp--;
				}
				int i, j;
				if (df >= 0) {
					i = (int)Math.ceil(f / (float)m) * m;
					j = (int)Math.floor((f + df) / (float)m) * m;
				} else {
					i = (int)Math.ceil((f + df) / (float)m) * m;
					j = (int)Math.floor(f / (float)m) * m;
				}
				float w = exp < 0 ? 2 : exp * 4;
				for (int k = Math.max(Math.abs(i), Math.abs(j)); k >= 10; k /= 10) w += 4;
				w = (w >= 8 ? 0.5F : 0.75F) * (float)(rad1 - rad0);
				for (; i <= j; i+=m) {
					double a = angle0 + ((double)i - f) / df * (angle1 - angle0);
					double sin = Math.sin(a), cos = Math.cos(a);
					float l = w * len(i, exp); double r = rad0 - (l * Math.abs(sin) + w * Math.abs(cos)) / 2D;
					float x = (float)(offset.x + r * sin) - l/2F, y = (float)(offset.y + r * cos), z = (float)offset.z;
					int k, e;
					if (i < 0) {
						quads.add(digit(x, y, z, w, -1, side, color));
						x += .6F * w; k = -i;
					} else k = i;
					if (k == 0) e = 0;
					else for(e = exp; k % 10 == 0; k /= 10) e++;
					drawNumber(quads, x, y, z, w, k, e, color, side);
				}
			} {
				float w = (float)(rad1 - rad0) * 0.25F;
				float x = (float)offset.x, y = (float)offset.y, z = (float)offset.z;
				drawPin(quads, x, y, z, w, side);
				double da = (angle1 - angle0) * 0.046875, a = angle0 - da, r = (rad0 + rad1) / 2.; w /= 2;
				drawPin(quads, x + (float)(r * Math.sin(a)), y + (float)(r * Math.cos(a)), z, w, side);
				a = angle1 + da;
				drawPin(quads, x + (float)(r * Math.sin(a)), y + (float)(r * Math.cos(a)), z, w, side);
			}
			//re-orient
			ModelRotation rot = o.getModelRotation();
			for (; q < quads.size(); q++) {
				data = quads.get(q).getVertexData();
				Util.rotate(data, 0, rot);
				Util.rotate(data, 7, rot);
				Util.rotate(data, 14, rot);
				Util.rotate(data, 21, rot);
			}
		}

	}

}
