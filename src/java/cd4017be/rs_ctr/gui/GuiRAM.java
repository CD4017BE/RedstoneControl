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
import static org.lwjgl.input.Keyboard.*;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.tileentity.RAM;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/** @author cd4017be */
public class GuiRAM extends ModularGui {

	private static final ResourceLocation TEX
	= new ResourceLocation(Main.ID, "textures/gui/ram.png");

	private final RAM tile;

	public GuiRAM(RAM tile, EntityPlayer player) {
		super(tile.getContainer(player, 0));
		this.tile = tile;
		GuiFrame frame = new GuiFrame(this, 160, 135, 3).background(TEX, 0, 0).title("gui.rs_ctr.ram.name", 0.5F);
		new FormatText(frame, 64, 7, 8, 16, "\\0x%08X", ()-> new Object[] {tile.writeIN}).align(0.5F).color(0xff00007f);
		new FormatText(frame, 64, 7, 88, 16, "\\0x%08X", ()-> new Object[] {tile.readIN}).align(0.5F).color(0xff007f00);
		new Editor(frame, 144, 102, 8, 25);
		compGroup = frame;
	}


	class Editor extends GuiCompBase<GuiCompGroup> {

		int cursor = -1;

		public Editor(GuiCompGroup parent, int w, int h, int x, int y) {
			super(parent, w, h, x, y);
		}

		@Override
		public void drawOverlay(int mx, int my) {
			if (my - y > 6) return;
			compGroup.drawTooltip(
				TooltipUtil.format("gui.rs_ctr.ram.layout", 32 >> (tile.mode & 3), tile.addrMask + 1),
				mx, my
			);
		}

		@Override
		public void drawBackground(int mx, int my, float t) {
			int bits = tile.mode & 3;
			compGroup.drawRect(x, y, 0, 232 + bits * 6, 128, 6);
			print(128, 0, tile.addrMask, 4, 4);
			int addr = tile.page << bits + 6, l = (Math.min(64, tile.memSize() + 3) >> 2) * 6;
			for(int i = 6, j = 0; i <= l; i += 6) {
				print(128, i, addr + j, 4, 5);
				for(int k = (4 << bits) - 1; k >= 0; k--, j++)
					print(
						k << 5 - bits, i, j <= tile.addrMask ? tile.get(j) : 0, 8 >> bits,
						(addr + j == tile.readIN ? 2:0) | (addr + j == tile.writeIN ? 1:0)
					);
			}
			if(cursor >= 0) {
				int y = this.y + 6 + (cursor >> 5) * 6;
				int x = this.x + 124 - (cursor & 31) * 4;
				int v = (tile.memory[cursor >> 3 & 63] >> (cursor << 2 & 28) & 15) * 8;
				if((cursor & 7 >> bits) == 0) v += 4;
				compGroup.drawRect(x, y, v, 190, 4, 6);
			}
		}

		private void print(int x, int y, int v, int w, int c) {
			x += this.x + 4 * (w - 1);
			y += this.y;
			c = 196 + c * 6;
			compGroup.drawRect(x, y, (v & 15) * 8 + 4, c, 4, 6);
			for(w--; w > 0; w--)
				compGroup.drawRect(x -= 4, y, ((v >>= 4) & 15) * 8, c, 4, 6);
		}

		@Override
		public boolean keyIn(char c, int k, byte d) {
			byte v;
			switch(k) {
			case KEY_UP: cursor -= 31;
			case KEY_RIGHT:
				if (--cursor < 0)
					cursor = mvPage(-1) ? cursor + 512 : 0;
				if (isShiftKeyDown()) cursor &= ~(7 >> (tile.mode & 3));
				return true;
			case KEY_DOWN: cursor += 31;
			case KEY_LEFT:
				if (isShiftKeyDown()) cursor |= 7 >> (tile.mode & 3);
				if (++cursor > 511)
					cursor = mvPage(1) ? cursor - 512 : 511;
				return true;
			case KEY_PRIOR: mvPage(-1); return true;
			case KEY_NEXT: mvPage(1); return true;
			case KEY_HOME:
				cursor = 0;
				sendPkt(RAM.A_PAGE, (byte)0);
				return true;
			case KEY_END:
				cursor = 511;
				sendPkt(RAM.A_PAGE, (byte)127);
				return true;
			case KEY_0: v = 0; break;
			case KEY_1: v = 1; break;
			case KEY_2: v = 2; break;
			case KEY_3: v = 3; break;
			case KEY_4: v = 4; break;
			case KEY_5: v = 5; break;
			case KEY_6: v = 6; break;
			case KEY_7: v = 7; break;
			case KEY_8: v = 8; break;
			case KEY_9: v = 9; break;
			case KEY_A: v = 10; break;
			case KEY_B: v = 11; break;
			case KEY_C: v = 12; break;
			case KEY_D: v = 13; break;
			case KEY_E: v = 14; break;
			case KEY_F: v = 15; break;
			default: return false;
			}
			sendPkt(RAM.A_SET_MEM, (short)(cursor | tile.page << 9), v);
			if (isCtrlKeyDown()) cursor += 31;
			if (++cursor > 511)
				cursor = mvPage(1) ? cursor - 512 : 511;
			return true;
		}

		@Override
		public boolean mouseIn(int mx, int my, int b, byte d) {
			mx = 31 - (mx - x) / 4;
			my = (my - y) / 6 - 1;
			if (d == A_DOWN) {
				if (my < 0)
					sendPkt(RAM.A_MODE, (byte)(tile.mode + (b == B_LEFT ? 1 : 3)));
				cursor = my * 32 + (mx < 0 ? 0 : mx);
			} else if (d == A_SCROLL)
				mvPage(-b);
			return true;
		}

		private boolean mvPage(int incr) {
			incr += tile.page;
			if (incr < 0 || incr >= 1 << ((tile.mode >> 4 & 15) - 6)) return false;
			sendPkt(RAM.A_PAGE, (byte)incr);
			return true;
		}

		@Override
		public boolean focus() {
			return true;
		}

		@Override
		public void unfocus() {
			cursor = -1;
		}

	}

	void saveFile(File file, ByteBuffer data) throws IOException {

	}

	void loadFile(File file, ByteBuffer data) throws IOException {
		String type = file.getName();
		int p = type.lastIndexOf('.');
		if(p >= 0) type = type.substring(p + 1);
		else type = "";
		try(FileInputStream fis = new FileInputStream(file)) {
			switch(type) {
			case "hex":
				loadHex(fis, data);
				return;
			case "bin":
				int n = (int)Math.min(fis.getChannel().size(), data.capacity());
				while(n > 0)
					n -= fis.getChannel().read(data);
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
		while((d = is.read()) >= 0) {
			if(d != ':') continue;
			int n = readHexDigits(is, 2);
			int addr = readHexDigits(is, 4);
			switch(readHexDigits(is, 2)) {
			case 0:
				data.position(addr);
				for(; n >= 4; n -= 4)
					data.putInt(readHexDigits(is, 8));
				for(; n > 0; n--)
					data.put((byte)readHexDigits(is, 2));
				break;
			case 1:
				return;
			default:
				throw new IOException();
			}
		}
	}

	static int readHexDigits(InputStream is, int n) throws IOException {
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

	/** @param mode = 0...7 | ch0 << 8 | ch1 << 10 | ch2 << 12 | ch3 << 14 */
	static void loadImg(BufferedImage img, ByteBuffer data, int mode) {
		data.order(ByteOrder.LITTLE_ENDIAN);
		int w = img.getWidth(), h = img.getHeight(), cap = data.capacity();
		int size = sizes[mode & 15];
		h = Math.min(h, (cap << 3) / size / w);
		for(int l = w * h, i = 0; i < l;) {
			int acc = 0;
			for(int j = 0; j < 32 && i < l; j += size, i++)
				acc |= decode(mode, img.getRGB(i % w, i / w)) << j;
			data.putInt(acc);
		}
	}

	private static int decode(int mode, int c) {
		switch(mode & 7) {
		case M_IDX1:
			return (c & 0x808080) != 0 ? 1 : 0;
		case M_IDX2:
			return (c & 0x80) != 0 ? 3
				: (c & 0x8000) != 0 ? 2 : (c & 0x800000) != 0 ? 1 : 0;
		case M_IDX4:
			return ((c & 0x7f) + (c >> 8 & 0x7f) + (c >> 16 & 0x7f)) / 191 << 3
			| c >> 7 & 1 | c >> 14 & 2 | c >> 21 & 4;
		case M_IDX8:
			return ((c & 0x3f) + (c >> 8 & 0x3f) + (c >> 16 & 0x3f)) / 48 << 6
			| c >> 6 & 3 | c >> 12 & 12 | c >> 18 & 48;
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
		default:
			return 0;
		}
	}

	static final byte[] sizes = {
		1, 2, 4, 8, 8, 8, 8, 8, 16, 16, 16, 16, 32, 0, 0, 0
	};
	static final byte M_IDX1 = 0, M_IDX2 = 1, M_IDX4 = 2, M_IDX8 = 3, M_A = 4,
	M_R = 5, M_G = 6, M_B = 7, M_AR = 8, M_RG = 9, M_GB = 10, M_BA = 11,
	M_ARGB = 12;

}
