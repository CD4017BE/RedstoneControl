package cd4017be.rs_ctr.gui;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import static org.lwjgl.input.Keyboard.*;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.FileBrowser;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.tileentity.RAM;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/** @author cd4017be */
public class GuiRAM extends ModularGui {

	private static final ResourceLocation TEX
	= new ResourceLocation(Main.ID, "textures/gui/ram.png");
	private static final File dir
	= new File(Minecraft.getMinecraft().mcDataDir, "circuitSchematics/data");

	private final RAM tile;
	private int fmtW, fmtH;

	public GuiRAM(RAM tile, EntityPlayer player) {
		super(tile.getContainer(player, 0));
		this.tile = tile;
		GuiFrame frame = new GuiFrame(this, 160, 144, 6).background(TEX, 0, 0).title("gui.rs_ctr.ram.name", 0.5F);
		new FormatText(frame, 64, 7, 8, 16, "\\0x%08X", ()-> new Object[] {tile.writeIN}).align(0.5F).color(0xff00007f);
		new FormatText(frame, 64, 7, 88, 16, "\\0x%08X", ()-> new Object[] {tile.readIN}).align(0.5F).color(0xff007f00);
		new Editor(frame, 144, 102, 8, 25);
		new Button(frame, 18, 9, 7, 128, 0, null, (i)-> {
			File file = new File(Minecraft.getMinecraft().mcDataDir, "circuitSchematics/data").getAbsoluteFile();
			file.mkdirs();
			file = new File(file, "ram.hex");
			GuiFrame fb = new FileBrowser(frame, this::importData, null).setFile(file).title("gui.rs_ctr.import_file", 0.5F);
			fb.init(width, height, zLevel, fontRenderer);
			fb.position(8, 8);
		}).tooltip("gui.rs_ctr.import_file");
		new Button(frame, 18, 9, 25, 128, 0, null, (i)-> sendPkt(RAM.A_DOWNLOAD)).tooltip("gui.rs_ctr.export_file");
		new TextField(frame, 48, 7, 54, 129, 8, ()-> Integer.toString(fmtW), (t)-> {
			try {
				fmtW = Integer.parseInt(t);
				if (fmtW <= 0) fmtW = 1;
				else if (fmtW > 4096) fmtW = 4096;
				fmtH = Math.max(1, Math.min(4096, (tile.addrMask + 1) / fmtW));
			} catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.fmt_w");
		new TextField(frame, 48, 7, 104, 129, 8, ()-> Integer.toString(fmtH), (t)-> {
			try {
				fmtH = Integer.parseInt(t);
				if (fmtH <= 0) fmtH = 1;
				else if (fmtH > 4096) fmtH = 4096;
			} catch(NumberFormatException e) {}
		}).tooltip("gui.rs_ctr.fmt_h");
		compGroup = frame;
		fmtW = 16;
		fmtH = tile.addrMask + 1 >> 4;
	}

	public void processDownload(byte[] mem) {
		ByteBuffer data = ByteBuffer.wrap(mem);
		File file = dir.getAbsoluteFile();
		file.mkdirs();
		file = new File(file, "ram.hex");
		GuiFrame f = new FileBrowser((GuiFrame)compGroup, (fb)-> {
			fb.close();
			exportData(fb.getFile(), data);
		}, null).setFile(file).title("gui.rs_ctr.export_file", 0.5F);
		f.init(width, height, zLevel, fontRenderer);
		f.position(8, 8);
	}

	private void exportData(File file, ByteBuffer data) {
		if (!file.getAbsolutePath().startsWith(dir.getAbsolutePath())) {
			sendChat(TooltipUtil.format("msg.rs_ctr.dir_invalid", file));
			return;
		}
		String name = file.getName();
		try(FileOutputStream fos = new FileOutputStream(file)) {
			if (name.endsWith(".hex")) {
				sendChat(TooltipUtil.format("msg.rs_ctr.encode_hex", file));
				if (fmtW > 255) fmtW = 255;
				writeHex(fos, data, fmtW);
			} else if (name.endsWith(".png")) {
				int bits = 5 - (tile.mode & 3);
				sendChat(TooltipUtil.format("msg.rs_ctr.encode_img", fmtW, fmtH, 1 << bits, file));
				BufferedImage img = new BufferedImage(fmtW, fmtH, BufferedImage.TYPE_INT_ARGB);
				saveImg(img, data, bits);
				ImageIO.write(img, "PNG", fos);
			} else {
				sendChat(TooltipUtil.format("msg.rs_ctr.encode_bin", file));
				for (int n = data.capacity(); n > 0;)
					n -= fos.getChannel().write(data);
			}
		} catch(FileNotFoundException e) {
			sendChat(TooltipUtil.format("msg.rs_ctr.no_file", file));
			return;
		} catch(Exception e) {
			e.printStackTrace();
			sendChat("\u00a74" + e.toString());
			return;
		}
		sendChat(TooltipUtil.format("msg.rs_ctr.export_succ"));
	}

	private void importData(FileBrowser fb) {
		fb.close();
		File file = fb.getFile();
		String name = file.getName();
		try(FileInputStream fis = new FileInputStream(file)) {
			int n = tile.memSize() << 2;
			PacketBuffer buff = GuiNetworkHandler.preparePacket(container);
			buff.writeByte(RAM.A_UPLOAD);
			if (name.endsWith(".hex")) {
				ByteBuffer data = ByteBuffer.allocate(n);
				sendChat(TooltipUtil.format("msg.rs_ctr.decode_hex", file));
				loadHex(fis, data);
				data.clear();
				buff.writeBytes(data);
			} else if (name.endsWith(".png")) {
				int bits = 5 - (tile.mode & 3);
				sendChat(TooltipUtil.format("msg.rs_ctr.decode_img", 1 << bits, file));
				BufferedImage img = ImageIO.read(fis);
				fmtW = img.getWidth();
				fmtH = img.getHeight();
				loadImg(img, buff, n, bits);
			} else {
				sendChat(TooltipUtil.format("msg.rs_ctr.decode_bin", file));
				n = (int)Math.min(fis.getChannel().size(), n);
				buff.writeShort(n + 3 >> 2);
				buff.ensureWritable(n + 3 & ~3);
				int o = buff.writerIndex(), m = o + n;
				while(o < m)
					o += fis.read(buff.array(), o, m - o);
				buff.writerIndex(o);
				while((n & 3) != 0) {
					n++;
					buff.writeByte(0);
				}
			}
			GuiNetworkHandler.GNH_INSTANCE.sendToServer(buff);
		} catch(FileNotFoundException e) {
			sendChat(TooltipUtil.format("msg.rs_ctr.no_file", file));
		} catch(Exception e) {
			e.printStackTrace();
			sendChat("\u00a74" + e.toString());
		}
	}

	static void loadHex(InputStream is, ByteBuffer data) throws IOException {
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

	static void writeHex(OutputStream os, ByteBuffer data, int m) throws IOException {
		int n;
		while((n = data.remaining()) > 0) {
			if (n > m) n = m;
			checkempty: {
				data.mark();
				for (int i = n; i > 0; i--)
					if (data.get() != 0) {
						data.reset();
						break checkempty;
					}
				continue;
			}
			os.write(':');
			writeHexByte(os, n);
			writeHexByte(os, data.position() >> 8);
			writeHexByte(os, data.position());
			writeHexByte(os, 0);
			for (;n > 0; n--)
				writeHexByte(os, data.get());
			os.write('\n');
		}
		os.write(":00000001\n".getBytes(StandardCharsets.US_ASCII));
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

	static void writeHexByte(OutputStream os, int v) throws IOException {
		os.write(Character.forDigit(v >> 4 & 15, 16));
		os.write(Character.forDigit(v & 15, 16));
	}

	static void loadImg(BufferedImage img, PacketBuffer data, int cap, int bits) {
		int w = img.getWidth(), h = img.getHeight();
		h = Math.min(h, (cap << 3 >> bits) / w);
		data.writeShort((w * h << bits) + 31 >> 5);
		switch(bits) {
		case 0://1 bit per pixel
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
		case 1://2 bit per pixel
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
		case 2://4 bit per pixel
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
		case 3://8 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					data.writeByte((byte)img.getRGB(x, y));
			for (int i = w * h; (i & 3) != 0; i++)
				data.writeByte(0);
			break;
		case 4://16 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					data.writeShortLE((short)img.getRGB(x, y));
			if ((w * h & 1) != 0) data.writeShortLE(0);
			break;
		case 5://32 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					data.writeIntLE(img.getRGB(x, y));
			break;
		}
	}

	private static final int[] COLORS4 = {
		0xff000000, 0xff0000ff, 0xff00ff00, 0xffff0000
	};
	private static final int[] COLORS16 = {
		0xff000000, 0xff0000bf, 0xff00bf00, 0xff00aaaa,
		0xffbf0000, 0xffaa00aa, 0xffbf8000, 0xffaaaaaa,
		0xff555555, 0xff5555ff, 0xff55ff55, 0xff00ffff,
		0xffff5555, 0xffff00ff, 0xffffff00, 0xffffffff
	};

	static void saveImg(BufferedImage img, ByteBuffer data, int bits) {
		data.order(ByteOrder.LITTLE_ENDIAN);
		int w = img.getWidth(), h = img.getHeight();
		h = Math.min(h, (data.capacity() << 3 >> bits) / w);
		switch(bits) {
		case 0://1 bit per pixel
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
		case 1://2 bit per pixel
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
		case 2://4 bit per pixel
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
		case 3://8 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					img.setRGB(x, y, 0xff000000 | data.get() & 0xff);
			break;
		case 4://16 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					img.setRGB(x, y, 0xff000000 | data.getShort() & 0xffff);
			break;
		case 5://32 bit per pixel
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					img.setRGB(x, y, data.getInt());
			break;
		}
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

}
