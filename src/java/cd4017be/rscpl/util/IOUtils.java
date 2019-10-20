package cd4017be.rscpl.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.objectweb.asm.Type;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import net.minecraft.util.ResourceLocation;

public class IOUtils {

	public static JsonReader readJson(InputStream is) {
		return new JsonReader(new InputStreamReader(is));
	}

	public static InputStream getClassResource(ResourceLocation rl, String suffix) {
		return getClassResource(rl, "/", suffix);
	}

	public static InputStream getClassResource(ResourceLocation rl, String basepath, String suffix) {
		return IOUtils.class.getResourceAsStream("/assets/" + rl.getResourceDomain() + basepath + rl.getResourcePath() + suffix);
	}

	public static Object nextValue(JsonReader jr) throws IOException {
		JsonToken jt = jr.peek();
		switch(jt) {
		case BOOLEAN: return Boolean.valueOf(jr.nextBoolean());
		case NULL: return null;
		case STRING: return jr.nextString();
		case NUMBER: return (Integer)jr.nextInt();
		default: throw new IllegalArgumentException("unsupported value type " + jt);
		}
	}

	public static Type getValidType(String s) {
		try {
			Type t = Type.getType(s);
			if (t.getSort() != Type.METHOD)
				return t;
		} catch (ArrayIndexOutOfBoundsException e) {}
		throw new IllegalArgumentException("invalid type descriptor: " + s);
	}

}
