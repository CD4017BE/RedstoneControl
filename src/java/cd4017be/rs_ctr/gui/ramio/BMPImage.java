package cd4017be.rs_ctr.gui.ramio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.network.PacketBuffer;

/**
 * @author CD4017BE */
public class BMPImage implements RAMImageFormat {

	{REGISTRY.add(this);}

	@Override
	public boolean applies(String filename) {
		return filename.endsWith(".bmp");
	}

	@Override
	public String infoMessage(boolean export, String file, int w, int h, int bits) {
		return export ?
			TooltipUtil.format("msg.rs_ctr.encode_bmp", w * bits, h, file) :
			TooltipUtil.format("msg.rs_ctr.decode_bmp", file);
	}

	@Override
	public void importFile(FileInputStream fis, PacketBuffer data, int[] wh, int bits, int cap) throws IOException {
		PNG_IMAGE.importFile(fis, data, wh, 1, cap);
		wh[0] /= bits;
	}

	@Override
	public void exportFile(FileOutputStream fos, ByteBuffer data, int w, int h, int bits) throws IOException {
		w *= bits;
		h = Math.min(h, ((data.capacity() << 3) - 1) / w + 1);
		final int offset = 62, line = (w + 31 >> 3 & ~3), sizeIm = line * h;
		//create Bitmap info header
		ByteBuffer header = ByteBuffer.allocate(offset).order(ByteOrder.LITTLE_ENDIAN);
		header.putShort((short)0x4D42).putInt(offset + sizeIm).putInt(0).putInt(offset)
			.putInt(40).putInt(w).putInt(-h).putShort((short)1).putShort((short)1)
			.putInt(0).putInt(sizeIm).putInt(0).putInt(0).putInt(0).putInt(0);
		header.putInt(0x000000).putInt(0xffffff); //color palette
		header.flip();
		fos.getChannel().write(header);
		//convert pixel data
		int ds = (line << 3) - w;
		if (ds == 0) { //data is 32bit aligned -> can be written directly
			if (sizeIm < data.capacity()) data.limit(sizeIm);
			while(data.hasRemaining()) {
				int i = Integer.reverse(data.getInt());
				fos.write(i);
				fos.write(i >> 8);
				fos.write(i >> 16);
				fos.write(i >> 24);
			}
			for (int i = sizeIm - data.limit(); i > 0; i--)
				fos.write(0);
		} else { //bit shifting required
			int shift = 0;
			long carry = 0;
			for (int y = h - 1; y >= 0; y--) {
				for (int x = (line >> 2) - 1; x >= 0; x--) {
					carry <<= 32;
					if (data.hasRemaining() && (x > 0 || shift + ds < 32))
						carry |= data.getInt() & 0xffffffffL;
					int i = Integer.reverse((int)(carry >> shift));
					fos.write(i);
					fos.write(i >> 8);
					fos.write(i >> 16);
					fos.write(i >> 24);
				}
				if ((shift += ds) >= 32) {
					shift -= 32;
					carry >>>= 32;
				}
			}
		}
	}
}