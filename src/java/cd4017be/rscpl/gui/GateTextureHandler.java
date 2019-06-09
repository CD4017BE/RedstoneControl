package cd4017be.rscpl.gui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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

	private static final Field TextureMap_mapRegisteredSprites;
	private static final String TINY_FONT = "rs_ctr:tiny_font";
	public static final ResourceLocation GATE_ICONS_LOC = new ResourceLocation(Main.ID, "textures/gates");
	public static final TextureMap GATE_ICONS_TEX = new TextureMap(GATE_ICONS_LOC.getResourcePath(), new GateTextureHandler());
	public static final ArrayList<Category> ins_sets = new ArrayList<>();
	private static boolean registered;

	static {
		Field f = null;
		try {
			try {f = TextureMap.class.getDeclaredField("field_110574_e");
			} catch (NoSuchFieldException e) {
				try {f = TextureMap.class.getDeclaredField("mapRegisteredSprites");
				} catch (NoSuchFieldException e1) {}
			}
			f.setAccessible(true);
		} catch(SecurityException e) {
			f = null;
			e.printStackTrace();
		}
		TextureMap_mapRegisteredSprites = f;
	}

	public static void register() {
		if (registered) return;
		registered = true;
		Minecraft.getMinecraft().renderEngine.loadTickableTexture(GATE_ICONS_LOC, GATE_ICONS_TEX);
	}

	@SuppressWarnings("unchecked")
	private void removeIllegallyRegisteredTextures(TextureMap textureMap) {
		if (TextureMap_mapRegisteredSprites == null) return;
		HashSet<String> authorizedMods = new HashSet<>();
		authorizedMods.add("minecraft"); //missingno
		for (Category c : ins_sets)
			for (BoundingBox2D<GateType<?>> g : c.instructions)
				authorizedMods.add(new ResourceLocation(g.owner.name).getResourceDomain());
		try {
			HashSet<String> badMods = new HashSet<>();
			for (Iterator<String> it = ((Map<String, TextureAtlasSprite>)TextureMap_mapRegisteredSprites.get(textureMap)).keySet().iterator(); it.hasNext();) {
				String modId = new ResourceLocation(it.next()).getResourceDomain();
				if (authorizedMods.contains(modId)) continue;
				badMods.add(modId);
				it.remove();
			}
			if (!badMods.isEmpty()) {
				StringBuilder sb = new StringBuilder("The following mods attempted to register textures into a texture atlas they don't belong:\n");
				for (String modId : badMods) sb.append("- ").append(modId).append("\n");
				sb.append("Modders please check whether the TextureMap given in texture stitch event is actually the one you want to register to!");
				Main.LOG.fatal(sb.toString());
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void registerSprites(TextureMap textureMap) {
		removeIllegallyRegisteredTextures(textureMap);
		textureMap.setTextureEntry(new RectangularSprite(TINY_FONT));
		for (Category ins : ins_sets) {
			textureMap.registerSprite(new ResourceLocation(ins.getIcon()));
			for (BoundingBox2D<GateType<?>> t : ins.instructions)
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
