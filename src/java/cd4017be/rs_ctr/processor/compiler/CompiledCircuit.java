package cd4017be.rs_ctr.processor.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.api.circuitgraph.Input;
import cd4017be.rs_ctr.api.circuitgraph.Output;
import cd4017be.rs_ctr.processor.Circuit;
import cd4017be.rs_ctr.processor.CircuitLoader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;


/**
 * @author CD4017BE
 *
 */
public class CompiledCircuit extends CircuitLoader {

	String[] ioLabels;
	byte[] classCode;

	void addIOPins(Input[] inputs, Output[] outputs) {
		this.inputs = new int[inputs.length];
		this.outputs = new int[outputs.length];
		this.ioLabels = new String[inputs.length + outputs.length];
		int n = 0;
		for (Input i : inputs) {
			if (i.isInterrupt())
				interruptPins |= 1 << n;
			ioLabels[n++] = i.name();
		}
		for (Output o : outputs)
			ioLabels[n++] = o.name();
	}

	@Override
	public Circuit loadCode() {
		// TODO Auto-generated method stub
		return super.loadCode();
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

}
