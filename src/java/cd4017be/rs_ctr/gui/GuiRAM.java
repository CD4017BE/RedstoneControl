package cd4017be.rs_ctr.gui;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.rs_ctr.tileentity.RAM;
import net.minecraft.entity.player.EntityPlayer;


public class GuiRAM extends ModularGui {

	private final RAM tile;

	public GuiRAM(RAM tile, EntityPlayer player) {
		super(tile.getContainer(player, 0));
		this.tile = tile;
	}

	void saveFile(File file, ByteBuffer data) throws IOException {
		
	}

	void loadFile(File file, ByteBuffer data) throws IOException {
		String type = file.getName();
		int p = type.lastIndexOf('.');
		if (p >= 0) type = type.substring(p + 1);
		else type = "";
		try(FileInputStream fis = new FileInputStream(file)) {
			switch(type) {
			case "hex":
				loadHex(fis, data);
				return;
			case "bin":
				int n = (int)Math.min(fis.getChannel().size(), data.capacity());
				while(n > 0) n -= fis.getChannel().read(data);
				return;
			case "txt":
			case "png":
				loadImg(ImageIO.read(fis), data, 0);
				return;
			}
		}
	}

	static void loadHex(InputStream is, ByteBuffer data) throws IOException {
		data.order(ByteOrder.BIG_ENDIAN);
		int d;
		while ((d = is.read()) >= 0) {
			if (d != ':') continue;
			int n = readHexDigits(is, 2);
			int addr = readHexDigits(is, 4);
			switch(readHexDigits(is, 2)) {
			case 0:
				data.position(addr);
				for (;n >= 4; n -= 4)
					data.putInt(readHexDigits(is, 8));
				for (;n > 0; n--)
					data.put((byte)readHexDigits(is, 2));
				break;
			case 1: return;
			default: throw new IOException();
			}
		}
	}

	static int readHexDigits(InputStream is, int n) throws IOException {
		int r = 0;
		while(--n >= 0) {
			int d = is.read();
			if (d < 0) throw new EOFException();
			d = Character.digit(d, 16);
			if (d < 0) throw new NumberFormatException();
			r = r << 4 | d;
		}
		return r;
	}

	/**
	 * @param mode = 0...7 | ch0 << 8 | ch1 << 10 | ch2 << 12 | ch3 << 14
	 */
	static void loadImg(BufferedImage img, ByteBuffer data, int mode) {
		data.order(ByteOrder.LITTLE_ENDIAN);
		int w = img.getWidth(), h = img.getHeight(), cap = data.capacity();
		int size = sizes[mode & 15];
		h = Math.min(h, (cap<<3) / size / w);
		for (int l = w * h, i = 0; i < l;) {
			int acc = 0;
			for (int j = 0; j < 32 && i < l; j += size, i++)
				acc |= decode(mode, img.getRGB(i % w, i / w)) << j;
			data.putInt(acc);
		}
	}

	private static int decode(int mode, int c) {
		switch(mode & 7) {
		case M_IDX1:
			return (c & 0x808080) != 0 ? 1 : 0;
		case M_IDX2:
			return (c & 0x80) != 0 ? 3 : (c & 0x8000) != 0 ? 2 : (c & 0x800000) != 0 ? 1 : 0;
		case M_IDX4:
			return ((c & 0x7f) + (c >> 8 & 0x7f) + (c >> 16 & 0x7f)) / 191 << 3 | c >> 7 & 1 | c >> 14 & 2 | c >> 21 & 4;
		case M_IDX8:
			return ((c & 0x3f) + (c >> 8 & 0x3f) + (c >> 16 & 0x3f)) / 48 << 6 | c >> 6 & 3 | c >> 12 & 12 | c >> 18 & 48;
		case M_A:
			return c >> 24 & 0xff;
		case M_R:
			return c >> 16 & 0xff;
		case M_G:
			return c >> 8 & 0xff;
		case M_B:
			return c & 0xff;
		case M_AR:
			return c >> 16 & 0xffff;
		case M_RG:
			return c >> 8 & 0xffff;
		case M_GB:
			return c & 0xffff;
		case M_BA:
			return (c >> 24 | c << 8) & 0xffff;
		case M_ARGB:
			return c & 0xffffff;
		default: return 0;
		}
	}

	static final byte[] sizes = {1, 2, 4, 8, 8, 8, 8, 8, 16, 16, 16, 16, 32, 0, 0, 0};
	static final byte M_IDX1 = 0, M_IDX2 = 1, M_IDX4 = 2, M_IDX8 = 3, M_A = 4, M_R = 5, M_G = 6, M_B = 7, M_AR = 8, M_RG = 9, M_GB = 10, M_BA = 11, M_ARGB = 12;

}
