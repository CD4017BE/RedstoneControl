package cd4017be.rs_ctr.gui;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.lib.util.Orientation;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.render.WireRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class BlockButton implements IInteractiveComponent {

	final IntConsumer action; //server only
	final Supplier<String> getModel, getText; //client only
	Vec3d pos = Vec3d.ZERO;
	Orientation face = Orientation.N;
	float width, height;

	public BlockButton(IntConsumer action, Supplier<String> model, Supplier<String> text) {
		this.action = action;
		this.getModel = model;
		this.getText = text;
	}

	public BlockButton setSize(float width, float height) {
		this.width = width;
		this.height = height;
		return this;
	}

	/**
	 * @param x relative x
	 * @param y relative y
	 * @param z relative z
	 * @param face attachment side
	 * @param o rotation of the whole system
	 * @return this
	 */
	public BlockButton setLocation(double x, double y, double z, Orientation o) {
		this.pos = o.rotate(new Vec3d(x - 0.5F, y - 0.5F, z - 0.5F)).addVector(0.5, 0.5, 0.5);
		this.face = o;
		return this;
	}

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		return IInteractiveComponent.rayTraceFlat(start, dir, pos, face.front, width, height);
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		if (action == null) return false;
		action.accept((hit ? A_HIT : 0) | (player.isSneaking() ? A_SNEAKING : 0));
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void draw(World world, BlockPos pos, double x, double y, double z, int light, BufferBuilder buffer) {
		Vec3d p = this.pos;
		WireRenderer.instance.drawModel(buffer, (float)(x + p.x), (float)(y + p.y), (float)(z + p.z), face, light, getModel.get());
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		return Pair.of(pos, getText.get());
	}

	public static final int A_HIT = 1, A_SNEAKING = 2;

}
