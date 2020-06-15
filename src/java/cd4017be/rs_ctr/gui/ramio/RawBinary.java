package cd4017be.rs_ctr.gui.ramio;

import java.io.*;
import java.nio.ByteBuffer;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.network.PacketBuffer;

/** 
 * @author CD4017BE */
public final class RawBinary implements RAMImageFormat {

	@Override
	public boolean applies(String filename) {
		return true;
	}

	@Override
	public String infoMessage(boolean export, String file, int w, int h, int bits) {
		return TooltipUtil.format(export ? "msg.rs_ctr.encode_bin" : "msg.rs_ctr.decode_bin", file);
	}

	@Override
	public void importFile(FileInputStream fis, PacketBuffer data, int[] wh, int bits, int cap)
	throws IOException {
		cap = (int)Math.min(fis.getChannel().size(), cap << 2);
		data.writeShort(cap + 3 >> 2);
		data.ensureWritable(cap + 3 & ~3);
		int o = data.writerIndex(), m = o + cap;
		while(o < m)
			o += fis.read(data.array(), o, m - o);
		data.writerIndex(o);
		while((cap & 3) != 0) {
			cap++;
			data.writeByte(0);
		}
	}

	@Override
	public void exportFile(FileOutputStream fos, ByteBuffer data, int w, int h, int bits) throws IOException {
		for (int n = data.capacity(); n > 0;)
			n -= fos.getChannel().write(data);
	}
}