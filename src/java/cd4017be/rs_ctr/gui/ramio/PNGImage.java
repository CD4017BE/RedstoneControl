package cd4017be.rs_ctr.gui.ramio;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.network.PacketBuffer;

/** 
 * @author CD4017BE */
public final class PNGImage implements RAMImageFormat {

	{REGISTRY.add(this);}
	private final int[] COLORS4 = {
		0xff000000, 0xff0000ff, 0xff00ff00, 0xffff0000
	}, COLORS16 = {
		0xff000000, 0xff0000bf, 0xff00bf00, 0xff00aaaa,
		0xffbf0000, 0xffaa00aa, 0xffbf8000, 0xffaaaaaa,
		0xff555555, 0xff5555ff, 0xff55ff55, 0xff00ffff,
		0xffff5555, 0xffff00ff, 0xffffff00, 0xffffffff
	};

	@Override
	public boolean applies(String filename) {
		return filename.endsWith(".png");
	}

	@Override
	public String infoMessage(boolean export, String file, int w, int h, int bits) {
		return export ?
			TooltipUtil.format("msg.rs_ctr.encode_img", w, h, bits, file) :
			TooltipUtil.format("msg.rs_ctr.decode_img", bits, file);
	}

	@Override
	public void importFile(FileInputStream fis, PacketBuffer data, int[] wh, int bits, int cap) throws IOException {
		BufferedImage img = ImageIO.read(fis);
		int w = wh[0] = img.getWidth(), h = wh[1] = img.getHeight();
		h = Math.min(h, (cap << 5) / bits / w);
		data.writeShort((w * h * bits) + 31 >> 5);
		switch(bits) {
		case 1://1 bit per pixel
			for(int x = 0, y = 0; y < h;) {
				int acc = 0;
				for(int j = 0; j < 32; j++) {
					if ((img.getRGB(x, y) & 0x808080) != 0)
						acc |= 1 << j;
					if (++x < w) continue;
					x = 0;
					if (++y >= h) break;
				}
				data.writeIntLE(acc);
			}
			break;
		case 2://2 bit per pixel
			for(int x = 0, y = 0; y < h;) {
				int acc = 0;
				for(int j = 0; j < 32; j+=2) {
					acc |= 4 - (Integer.numberOfLeadingZeros(img.getRGB(x, y) & 0x808080) >> 3) << j;
					if (++x < w) continue;
					x = 0;
					if (++y >= h) break;
				}
				data.writeIntLE(acc);
			}
			break;
		case 4://4 bit per pixel
			for(int x = 0, y = 0; y < h;) {
				int acc = 0;
				for(int j = 0; j < 32; j+=4) {
					int c = img.getRGB(x, y);
					acc |= ((((c & 0x7f) + (c >> 8 & 0x7f) + (c >> 16 & 0x7f)) / 191) << 3
						| c >> 7 & 1 | c >> 14 & 2 | c >> 21 & 4) << j;
					if (++x < w) continue;
					x = 0;
					if (++y >= h) break;
				}
				data.writeIntLE(acc);
			}
			break;
		case 8://8 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					data.writeByte((byte)img.getRGB(x, y));
			for (int i = w * h; (i & 3) != 0; i++)
				data.writeByte(0);
			break;
		case 16://16 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					data.writeShortLE((short)img.getRGB(x, y));
			if ((w * h & 1) != 0) data.writeShortLE(0);
			break;
		case 32://32 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					data.writeIntLE(img.getRGB(x, y));
			break;
		}
	}

	@Override
	public void exportFile(FileOutputStream fos, ByteBuffer data, int w, int h, int bits) throws IOException {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		data.order(ByteOrder.LITTLE_ENDIAN);
		h = Math.min(h, (data.capacity() << 3) / bits / w);
		switch(bits) {
		case 1://1 bit per pixel
			for(int x = 0, y = 0; y < h;) {
				int acc = data.getInt();
				for(int j = 0; j < 32; j++) {
					img.setRGB(x, y, (acc >> j & 1) != 0 ? 0xffffffff : 0xff000000);
					if (++x < w) continue;
					x = 0;
					if (++y >= h) break;
				}
			}
			break;
		case 2://2 bit per pixel
			for(int x = 0, y = 0; y < h;) {
				int acc = data.getInt();
				for(int j = 0; j < 32; j+=2) {
					img.setRGB(x, y, COLORS4[acc >> j & 3]);
					if (++x < w) continue;
					x = 0;
					if (++y >= h) break;
				}
			}
			break;
		case 4://4 bit per pixel
			for(int x = 0, y = 0; y < h;) {
				int acc = data.getInt();
				for(int j = 0; j < 32; j+=4) {
					img.setRGB(x, y, COLORS16[acc >> j & 15]);
					if (++x < w) continue;
					x = 0;
					if (++y >= h) break;
				}
			}
			break;
		case 8://8 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					img.setRGB(x, y, 0xff000000 | data.get() & 0xff);
			break;
		case 16://16 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					img.setRGB(x, y, 0xff000000 | data.getShort() & 0xffff);
			break;
		case 32://32 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					img.setRGB(x, y, data.getInt());
			break;
		}
		ImageIO.write(img, "PNG", fos);
	}
}