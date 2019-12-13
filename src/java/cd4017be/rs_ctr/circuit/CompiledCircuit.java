package cd4017be.rs_ctr.circuit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.circuit.gates.Input;
import cd4017be.rs_ctr.circuit.gates.Output;
import cd4017be.rs_ctr.tileentity.Editor;
import cd4017be.rscpl.compile.CompiledProgram;
import cd4017be.rscpl.editor.InvalidSchematicException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;


/**
 * @author CD4017BE
 *
 */
public class CompiledCircuit extends UnloadedCircuit implements CompiledProgram {

	public InvalidSchematicException compileWarning;
	public String[] ioLabels;
	byte[] classCode;

	@Override
	public void setCode(byte[] code) {
		//now override the temporary class name with the actual hash name.
		//This is quite simple since we know it's stored in constant pool index 1,
		//also all names have the same length and only contain single byte UTF8 chars.
		this.ID = hash(code);
		int i = 13; //4 magic + 4 version + 2 pool size + 1 tagID + 2 string length
		for (char c : name(ID).toCharArray())
			code[i++] = (byte)c;
		this.classCode = code;
	}

	public void setIOPins(List<Input> inputs, List<Output> outputs) {
		this.inputs = new int[inputs.size()];
		this.outputs = new int[outputs.size()];
		this.ioLabels = new String[this.inputs.length + this.outputs.length];
		int n = 0;
		for (Input i : inputs) {
			if (Boolean.TRUE.equals(i.getParam(0)))
				interruptPins |= 1 << n;
			ioLabels[n++] = i.label;
			if (i.label.isEmpty())
				compileWarning = new InvalidSchematicException(Editor.MISSING_IO_LABEL, i, 0);
		}
		for (Output o : outputs) {
			ioLabels[n++] = o.label;
			if (o.label.isEmpty())
				compileWarning = new InvalidSchematicException(Editor.MISSING_IO_LABEL, o, 0);
		}
	}

	@Override
	public Circuit load() {
		if (ID == null) return this;
		String name = name(ID);
		CircuitLoader.INSTANCE.register(name, classCode);
		Circuit c = CircuitLoader.INSTANCE.newCircuit(name);
		if (c == null) return this;
		c.deserializeNBT(serializeNBT());
		return c;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setTag("labels", Utils.writeStringArray(ioLabels));
		if (classCode != null) try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
			GZIPOutputStream os = new GZIPOutputStream(bos);
			os.write(classCode);
			os.close();
			nbt.setByteArray("class", bos.toByteArray());
		} catch (IOException e) {e.printStackTrace();}
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		ioLabels = Utils.readStringArray(nbt.getTagList("labels", NBT.TAG_STRING), new String[inputs.length + outputs.length]);
		if (nbt.hasKey("class", NBT.TAG_BYTE_ARRAY)) 
			try (GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(nbt.getByteArray("class")))){
				ByteBuf buf = Unpooled.buffer(4096);
				while(buf.writeBytes(is, 4096) > 0);
				is.close();
				buf.readBytes(classCode = new byte[buf.readableBytes()]);
			} catch (IOException e) {
				e.printStackTrace();
				classCode = null;
			}
	}

	private static final HashFunction hashfunc = Hashing.murmur3_128();

	public static UUID hash(byte[] data) {
		ByteBuffer buf = ByteBuffer.wrap(hashfunc.hashBytes(data).asBytes());
		return new UUID(buf.getLong(), buf.getLong());
	}

}
