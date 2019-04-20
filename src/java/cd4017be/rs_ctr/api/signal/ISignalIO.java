package cd4017be.rs_ctr.api.signal;

import java.util.function.IntConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The signal system API interface to be implemented by devices offering communication ports.
 * @author CD4017BE
 */
public interface ISignalIO {

	/**
	 * @return a list of all SignalPorts provided by this device
	 */
	SignalPort[] getSignalPorts();

	/**
	 * @param pin the SignalPort's pin id
	 * @return the SignalPort for given pin
	 */
	default @Nullable SignalPort getSignalPort(int pin) {
		for (SignalPort port : getSignalPorts())
			if (port.pin == pin)
				return port;
		return null;
	}

	/**
	 * get the callback of the given sink/receiving port<dl>
	 * Warning: this method and the returned function may get called from within {@link SignalPort#onLoad()}
	 * so be careful when initializing Ports during {@link TileEntity#onLoad()} because blocks can't be accessed at that point.
	 * @param pin the SignalPort's pin id
	 * @return a function to call whenever the state of the transmitted signal changes
	 */
	@Nonnull IntConsumer getPortCallback(int pin);

	/**
	 * set the callback of the given source/sending port
	 * @param pin the SignalPort's pin id
	 * @param callback a function to call whenever the state of the transmitted signal changes.<br>
	 * null indicates that transmission is currently not possible (the receiver became unloaded)
	 */
	void setPortCallback(int pin, @Nullable IntConsumer callback);

	/**
	 * @param pin the SignalPort's pin id
	 * @param event what happened: {@link #E_CONNECT}, {@link #E_DISCONNECT}, {@link #E_CON_ADD}, {@link #E_CON_REM}
	 */
	void onPortModified(SignalPort port, int event);

	/**port was functionally connected to another port */
	public static final int E_CONNECT = 1;
	/**port was functionally disconnected from another port */
	public static final int E_DISCONNECT = 2;
	/**the port's connection object was set */
	public static final int E_CON_ADD = 20;
	/**the port's connection object was removed */
	public static final int E_CON_REM = 24;
	/**the port's connection state needs client sync */
	public static final int E_CON_UPDATE = 16;

	/**
	 * @param world
	 * @param pos
	 * @param pin
	 * @return
	 */
	public static SignalPort getPort(World world, BlockPos pos, int pin) {
		if (world == null) return null;
		if (pin >= 0) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof ISignalIO)
				return ((ISignalIO)te).getSignalPort(pin);
		} else for (Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos), (e)-> e instanceof ISignalIO)) {
			SignalPort port = ((ISignalIO)e).getSignalPort(pin);
			if (port != null) return port;
		}
		return null;
	}

}
