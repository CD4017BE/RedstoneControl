package cd4017be.rs_ctr.circuit;

import java.util.Random;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/** 
 * @author CD4017BE */
public class UtilFunc {

	private static final Random RNG = new Random();
	public static long lastTick;

	public static float floor(float x) {
		int i = Float.floatToRawIntBits(x);
		int e = 150 - (i >> 23 & 0xff);
		if (e <= 0) return x;
		if (e > 23) return x >= 0 ? 0 : -1F;
		if (i >= 0) i = i >> e;
		else i = (i - 1 >> e) + 1;
		return Float.intBitsToFloat(i << e);
	}

	public static float ceil(float x) {
		int i = Float.floatToRawIntBits(x);
		int e = 150 - (i >> 23 & 0xff);
		if (e <= 0) return x;
		if (e > 23) return x <= 0 ? 0 : 1F;
		if (i < 0) i = i >> e;
		else i = (i - 1 >> e) + 1;
		return Float.intBitsToFloat(i << e);
	}

	public static float sqrt(float x) {
		return (float)Math.sqrt(x);
	}

	public static float random() {
		return RNG.nextFloat();
	}

	public static int random(int range) {
		try {
			return RNG.nextInt(range);
		} catch(IllegalArgumentException e) {
			throw new ArithmeticException("bound <= 0");
		}
	}

	public static long systemTime() {
		return lastTick;
	}

	public static long worldTime() {
		WorldServer world = DimensionManager.getWorld(0);
		return world != null ? world.worldInfo.totalTime : 0L;
	}

	public static long dayTime() {
		WorldServer world = DimensionManager.getWorld(0);
		return world != null ? world.worldInfo.getWorldTime() : 0L;
	}

	static {
		MinecraftForge.EVENT_BUS.register(new UtilFunc());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onTick(TickEvent.ServerTickEvent e) {
		if (e.phase == Phase.START) lastTick = System.currentTimeMillis();
	}

}
