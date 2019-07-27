package cd4017be.rscpl.gui;

import java.util.ArrayList;
import cd4017be.lib.render.ModTextureMap;
import cd4017be.lib.render.RectangularSprite;
import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.editor.BoundingBox2D;
import cd4017be.rscpl.editor.GateType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
@SideOnly(Side.CLIENT)
public class GateTextureHandler implements ITextureMapPopulator {

	private static final String TINY_FONT = "rs_ctr:tiny_font";
	public static final ResourceLocation GATE_ICONS_LOC = new ResourceLocation(Main.ID, "textures/gates");
	public static final ModTextureMap GATE_ICONS_TEX = new ModTextureMap(GATE_ICONS_LOC.getResourcePath(), new GateTextureHandler());
	public static final ArrayList<Category> ins_sets = new ArrayList<>();
	private static boolean registered;

	public static void register() {
		if (registered) return;
		registered = true;
		Minecraft.getMinecraft().renderEngine.loadTickableTexture(GATE_ICONS_LOC, GATE_ICONS_TEX);
	}

	@Override
	public void registerSprites(TextureMap textureMap) {
		textureMap.setTextureEntry(new RectangularSprite(TINY_FONT));
		for (Category ins : ins_sets) {
			textureMap.registerSprite(new ResourceLocation(ins.getIcon()));
			for (BoundingBox2D<GateType> t : ins.instructions)
				textureMap.setTextureEntry(new RectangularSprite(t.owner.getIcon()));
		}
	}

	public static void drawIcon(BufferBuilder b, int x, int y, int w, int h, String icon, double z) {
		TextureAtlasSprite tex = GATE_ICONS_TEX.getAtlasSprite(icon);
		x += (w - tex.getIconWidth()) / 2;
		y += (h - tex.getIconHeight()) / 2;
		int X = x + tex.getIconWidth(),
			Y = y + tex.getIconHeight();
		double u = tex.getMinU(), U = tex.getMaxU(),
				v = tex.getMinV(), V = tex.getMaxV();
		boolean t = tex instanceof RectangularSprite && ((RectangularSprite)tex).uvTransposed();
		b.pos(x, Y, z).tex(t ? U:u, t ? v:V).endVertex();
		b.pos(X, Y, z).tex(U, V).endVertex();
		b.pos(X, y, z).tex(t ? u:U, t ? V:v).endVertex();
		b.pos(x, y, z).tex(u, v).endVertex();
	}

	public static void drawTinyText(BufferBuilder b, String s, int x, int y, int w, double z) {
		char[] cs = s.toCharArray();
		double scale = cs.length <= w ? 1.0 : (double)cs.length / (double)w;
		double px = (double)x + ((double)w - (double)cs.length / scale) * 2.0, dx = 4.0 / scale;
		double y0 = (double)y + (scale - 1.0) * 1.25, y1 = y0 + 6.0 / scale;
		
		TextureAtlasSprite tex = GateTextureHandler.GATE_ICONS_TEX.getAtlasSprite(TINY_FONT);
		float du = 1F/16F, dv = 6F / (float)tex.getIconHeight();
		float[] t = new float[8];
		for (char c : cs) {
			RectangularSprite.getInterpolatedUV(t, tex, (float)(c & 15) * du, (float)(c >> 4) * dv, du, dv);
			double x1 = px + dx;
			b.pos(px, y0, z).tex(t[0], t[1]).endVertex();
			b.pos(px, y1, z).tex(t[2], t[3]).endVertex();
			b.pos(x1, y1, z).tex(t[4], t[5]).endVertex();
			b.pos(x1, y0, z).tex(t[6], t[7]).endVertex();
			px = x1;
		}
	}

}
