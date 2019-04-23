package cd4017be.rs_ctr.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.IntConsumer;

import cd4017be.lib.jvm_utils.ClassAssembler;
import cd4017be.lib.jvm_utils.ClassUtils;
import cd4017be.lib.jvm_utils.NBT2Class;
import cd4017be.lib.jvm_utils.SecurityChecker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * @author CD4017BE
 *
 */
public class CircuitLoader extends Circuit {

	private StateBuffer state;

	@Override
	public boolean tick() {return false;}

	@Override
	public void setState(StateBuffer state) {
		this.state = state;
	}

	@Override
	public StateBuffer getState() {
		return state;
	}

	@Override
	public Circuit loadCode() {
		String name = name(ID);
		ClassAssembler.INSTANCE.register(name, CircuitLoader::loadCircuitFile);
		Circuit c = ClassUtils.makeInstance(name, Circuit.class);
		if (c == null) return this;
		c.deserializeNBT(serializeNBT());
		return c;
	}

	public static final SecurityChecker CHECKER = new SecurityChecker()
			.putAll(IntConsumer.class)
			.putAll(String.class)
			.putAll(Math.class)
			.put(Object.class)
			.put(System.class, "arraycopy(Ljava.lang.Object;ILjava.lang.Object;II)V");

	public static Circuit create(NBT2Class gen) {
		UUID uid = gen.getHash();
		String name = name(uid);
		File file = file(name);
		if (file == null) //multiplayer client
			ClassAssembler.INSTANCE.register(name, gen);
		else if (file.exists()) //already saved
			ClassAssembler.INSTANCE.register(name, CircuitLoader::loadCircuitFile);
		else try { //to be generated
			byte[] data = gen.apply(name);
			FileOutputStream os = new FileOutputStream(file);
			os.write(data);
			os.close();
			gen.nbt.removeTag("cpt");
			gen.nbt.removeTag("methods");
			gen.nbt.removeTag("fields");
			ClassAssembler.INSTANCE.register(name, (n)-> data);
		} catch (IOException | IllegalArgumentException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
		return ClassUtils.makeInstance(name, Circuit.class);
	}

	public static String name(UUID uid) {
		return "C_" + uid.toString().replace('-', '_');
	}

	private static File file(String name) {
		FMLCommonHandler fml = FMLCommonHandler.instance();
		MinecraftServer server = fml.getMinecraftServerInstance();
		if (server == null) return null;
		File dir = new File(fml.getSavesDirectory(), server.getFolderName() + "/circuits");
		dir.mkdir();
		return new File(dir, name.substring(2) + ".class");
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
			CHECKER.verify(arr);
			return arr;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
