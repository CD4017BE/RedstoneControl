package cd4017be.rs_ctr.api.wire;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import cd4017be.rs_ctr.api.signal.IConnector;
import cd4017be.rs_ctr.api.signal.ISignalIO;
import cd4017be.rs_ctr.api.signal.MountedSignalPort;
import cd4017be.rs_ctr.api.signal.SignalPort;

/**
 * Parses a connected signal wire line from a given starting point.
 * @author cd4017be
 */
public class SignalLine implements Collection<MountedSignalPort> {

	/**start/end point of the connection (may be null if not a fully closed connection) */
	public final MountedSignalPort source, sink;
	/**list of all wire hooks in order source to sink */
	public final RelayPort[] hooks;

	/**
	 * Parses the connection of the given port.
	 * @param port
	 * @throws WireLoopException if the ports are connected in a loop
	 */
	public SignalLine(MountedSignalPort port) throws WireLoopException {
		ArrayDeque<RelayPort> list = new ArrayDeque<>();
		MountedSignalPort p0 = scan(port, list);
		MountedSignalPort p1 = port instanceof RelayPort ? scan(((RelayPort)port).opposite, list) : port;
		if (port.isMaster) {
			this.source = p1;
			this.sink = p0;
		} else {
			this.source = p0;
			this.sink = p1;
		}
		this.hooks = list.toArray(new RelayPort[list.size()]);
	}

	private static MountedSignalPort getLink(MountedSignalPort port) {
		IConnector c = port.getConnector();
		if (!(c instanceof IWiredConnector)) return null;
		IWiredConnector con = (IWiredConnector)c;
		SignalPort sp = ISignalIO.getPort(port.getWorld(), con.getLinkPos(), con.getLinkPin());
		if (!(sp instanceof MountedSignalPort) || !(sp.isMaster ^ port.isMaster)) return null;
		c = ((MountedSignalPort)sp).getConnector();
		if (!(c instanceof IWiredConnector)) return null;
		con = (IWiredConnector)c;
		if (!port.getPos().equals(con.getLinkPos()) || port.pin != con.getLinkPin()) return null;
		return (MountedSignalPort)sp;
	}

	private static MountedSignalPort scan(MountedSignalPort port, ArrayDeque<RelayPort> list) throws WireLoopException {
		boolean dir = port.isMaster;
		if (port instanceof RelayPort && port.getConnector() != null)
			if (dir) list.addLast((RelayPort)port);
			else list.addFirst((RelayPort)port);
		while ((port = getLink(port)) instanceof RelayPort) {
			RelayPort sr = (RelayPort)port;
			if (list.contains(sr)) throw new WireLoopException();
			if (dir) list.addLast(sr);
			else list.addFirst(sr);
			port = sr = sr.opposite;
			if (sr.getConnector() == null) return null;
			if (dir) list.addLast(sr);
			else list.addFirst(sr);
		}
		return port;
	}

	@Override
	public int size() {
		int n = hooks.length;
		if (source != null) n++;
		if (sink != null) n++;
		return n;
	}

	@Override
	public boolean isEmpty() {
		return source == null && sink == null && hooks.length == 0;
	}

	@Override
	public boolean contains(Object port) {
		if (port instanceof RelayPort) {
			for (RelayPort sr : hooks)
				if (sr == port) return true;
			return false;
		}
		return port == source || port == sink;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	@Override
	public Iterator<MountedSignalPort> iterator() {
		return new Iterator<MountedSignalPort>() {
			int i = source == null ? 0 : -1;
			int n = hooks.length + (sink == null ? 0 : 1);

			@Override
			public boolean hasNext() {
				return i < n;
			}

			@Override
			public MountedSignalPort next() {
				int j = i++;
				return j < 0 ? source : j < hooks.length ? hooks[j] : sink;
			}
		};
	}

	@Override
	public void forEach(Consumer<? super MountedSignalPort> action) {
		if (source != null)
			action.accept(source);
		for (RelayPort port : hooks)
			action.accept(port);
		if (sink != null)
			action.accept(sink);
	}

	@Override
	public MountedSignalPort[] toArray() {
		return toArray(new MountedSignalPort[size()]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		int n = size();
		if (a.length < n) a = (T[]) Array.newInstance(a.getClass().getComponentType(), n);
		int i = 0;
		if (source != null) a[i++] = (T) source;
		System.arraycopy(hooks, 0, a, i, hooks.length);
		if (source != null) a[n - 1] = (T) source;
		return null;
	}

	@Override
	public boolean add(MountedSignalPort e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends MountedSignalPort> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("serial")
	public static class WireLoopException extends Exception {}

}
