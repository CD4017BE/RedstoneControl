package cd4017be.rs_ctr.gui.ramio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.network.PacketBuffer;

/** 
 * @author CD4017BE */
public final class IntelHex implements RAMImageFormat {

	{REGISTRY.add(this);}

	@Override
	public boolean applies(String filename) {
		return filename.endsWith(".hex");
	}

	@Override
	public String infoMessage(boolean export, String file, int w, int h, int bits) {
		return TooltipUtil.format(export ? "msg.rs_ctr.encode_hex" : "msg.rs_ctr.decode_hex", file);
	}

	@Override
	public void importFile(FileInputStream fis, PacketBuffer data, int[] wh, int bits, int cap) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(cap << 2);
		int d;
		while((d = fis.read()) >= 0) {
			if(d != ':') continue;
			int n = readHexDigits(fis, 2);
			int addr = readHexDigits(fis, 4);
			switch(readHexDigits(fis, 2)) {
			case 0:
				buff.position(addr);
				for(; n >= 4; n -= 4)
					buff.putInt(readHexDigits(fis, 8));
				for(; n > 0; n--)
					buff.put((byte)readHexDigits(fis, 2));
				break;
			case 1:
				return;
			default:
				throw new IOException();
			}
		}
		buff.clear();
		data.writeBytes(buff);
	}

	private int readHexDigits(InputStream is, int n) throws IOException {
		int r = 0;
		while(--n >= 0) {
			int d = is.read();
			if(d < 0) throw new EOFException();
			d = Character.digit(d, 16);
			if(d < 0) throw new NumberFormatException();
			r = r << 4 | d;
		}
		return r;
	}

	@Override
	public void exportFile(FileOutputStream fos, ByteBuffer data, int w, int h, int bits) throws IOException {
		if (w > 255) w = 255;
		int n;
		while((n = data.remaining()) > 0) {
			if (n > w) n = w;
			checkempty: {
				data.mark();
				for (int i = n; i > 0; i--)
					if (data.get() != 0) {
						data.reset();
						break checkempty;
					}
				continue;
			}
			fos.write(':');
			writeHexByte(fos, n);
			writeHexByte(fos, data.position() >> 8);
			writeHexByte(fos, data.position());
			writeHexByte(fos, 0);
			for (;n > 0; n--)
				writeHexByte(fos, data.get());
			fos.write('\n');
		}
		fos.write(":00000001\n".getBytes(StandardCharsets.US_ASCII));
	}

	private void writeHexByte(OutputStream os, int v) throws IOException {
		os.write(Character.forDigit(v >> 4 & 15, 16));
		os.write(Character.forDigit(v & 15, 16));
	}
}