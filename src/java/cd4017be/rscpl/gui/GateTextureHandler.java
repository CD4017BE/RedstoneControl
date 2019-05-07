package cd4017be.rscpl.gui;

import java.util.ArrayList;

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

	public static final ResourceLocation GATE_ICONS_LOC = new ResourceLocation(Main.ID, "textures/gates");
	public static final TextureMap GATE_ICONS_TEX = new TextureMap(GATE_ICONS_LOC.getResourcePath(), new GateTextureHandler());
	public static final ArrayList<Category> ins_sets = new ArrayList<>();
	private static boolean registered;

	public static void register() {
		if (registered) return;
		registered = true;
		Minecraft.getMinecraft().renderEngine.loadTickableTexture(GATE_ICONS_LOC, GATE_ICONS_TEX);
	}

	@Override
	public void registerSprites(TextureMap textureMap) {
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

}
