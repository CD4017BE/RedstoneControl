package cd4017be.rs_ctr.circuit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Function;
import cd4017be.lib.jvm_utils.SecurityChecker;
import cd4017be.rs_ctr.Main;
import cd4017be.rscpl.util.StateBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * @author CD4017BE
 *
 */
public class CircuitLoader extends ClassLoader {

	public static final CircuitLoader INSTANCE = new CircuitLoader();
	/**
	 * A white-list of safe classes and methods that are allowed to appear inside the constant pool of a compiled circuit.
	 */
	public static final SecurityChecker CHECKER = new SecurityChecker()
			.putAll(Circuit.class)
			.putAll(StateBuffer.class)
			.putAll(String.class)
			.putAll(Math.class)
			.putAll(UtilFunc.class)
			.putAll(Integer.class)
			.putAll(Float.class)
			.put(Object.class);

	private HashMap<String, Function<String, byte[]>> registry = new HashMap<>();

	private CircuitLoader() {
		super(CircuitLoader.class.getClassLoader());
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Function<String, byte[]> gen = registry.remove(name);
		if (gen == null) throw new ClassNotFoundException(name);
		byte[] data = gen.apply(name);
		if (data == null) throw new ClassNotFoundException(name);
		CHECKER.verify(data);
		return defineClass(name, data, 0, data.length);
	}

	/**
	 * @param name implementation class name
	 * @return a new circuit instance
	 */
	public Circuit newCircuit(String name) {
		try {
			return (Circuit)Class.forName(name, true, this).newInstance();
		} catch(ClassNotFoundException | LinkageError | SecurityException e) {
			Main.LOG.error("failed to load circuit class", e);
			return null;
		} catch (InstantiationException | IllegalAccessException e) {
			Main.LOG.error("failed to initialize circuit instance", e);
			return null;
		}
	}

	/**
	 * Register the given circuit class for loading
	 * @param name UUID name of the circuit
	 * @param code class file code (null if to be loaded from world save)
	 * @return whether it was already registered
	 */
	public boolean register(String name, byte[] code) {
		if (findLoadedClass(name) != null) return true;
		Function<String, byte[]> gen;
		if (code != null) {
			gen = (n)-> code;
			File file = file(name);
			if (file != null && !file.exists())
				try {
					FileOutputStream os = new FileOutputStream(file);
					os.write(code);
					os.close();
				} catch (IOException | SecurityException e) {
					Main.LOG.error("Failed to save circuit class file", e);
				}
		} else gen = CircuitLoader::loadCircuitFile;
		return registry.put(name, gen) != null;
	}

	private static byte[] loadCircuitFile(String name) {
		try {
			File file = file(name);
			if (file == null) return null;
			FileInputStream is = new FileInputStream(file);
			ByteBuf buff = Unpooled.buffer();
			while(buff.writeBytes(is, 4096) == 4096);
			is.close();
			byte[] arr = new byte[buff.writerIndex()];
			buff.readBytes(arr);
			return arr;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static File file(String name) {
		FMLCommonHandler fml = FMLCommonHandler.instance();
		MinecraftServer server = fml.getMinecraftServerInstance();
		if (server == null) return null;
		File dir = new File(fml.getSavesDirectory(), server.getFolderName() + "/circuits");
		dir.mkdir();
		return new File(dir, name.substring(2) + ".class");
	}

}
