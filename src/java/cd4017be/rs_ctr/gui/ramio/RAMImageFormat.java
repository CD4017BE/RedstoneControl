package cd4017be.rs_ctr.gui.ramio;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import net.minecraft.network.PacketBuffer;

/** 
 * @author CD4017BE */
public interface RAMImageFormat {

	/**
	 * @param filename
	 * @return whether this format should be used to export/import the given file.
	 */
	boolean applies(String filename);

	/**
	 * @param export
	 * @param file
	 * @param w width
	 * @param h height
	 * @param bits memory layout: bits per word
	 * @return info message for export/import
	 */
	String infoMessage(boolean export, String file, int w, int h, int bits);

	/**
	 * @param fos file to export
	 * @param data memory content
	 * @param w width
	 * @param h height
	 * @param bits memory layout: bits per word
	 * @throws IOException
	 */
	void exportFile(FileOutputStream fos, ByteBuffer data, int w, int h, int bits) throws IOException;

	/**
	 * @param fis file to import
	 * @param data target buffer. Must start with the decoded size (in units of 32bit) as short.
	 * @param wh [0] = width, [1] = height
	 * @param bits memory layout: bits per word
	 * @param cap memory capacity (in units of 32bit)
	 * @throws IOException
	 */
	void importFile(FileInputStream fis, PacketBuffer data, int[] wh, int bits, int cap) throws IOException;


	ArrayList<RAMImageFormat> REGISTRY = new ArrayList<>();

	static RAMImageFormat get(String filename) {
		for (RAMImageFormat fmt : REGISTRY)
			if (fmt.applies(filename))
				return fmt;
		return RAW_BINARY;
	}

	RAMImageFormat RAW_BINARY = new RawBinary();
	RAMImageFormat INTEL_HEX = new IntelHex();
	RAMImageFormat PNG_IMAGE = new PNGImage();
	RAMImageFormat BMP_IMAGE = new BMPImage();
}
