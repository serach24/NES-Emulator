package me.charlesj.util.patternconvert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 2020/2/16.
 */
public class PaletteLoader {

    public static final int[][] DEFAULT_PALETTE = new int[256][4];

    static {
        for (int i=0; i<256; i++) {
            DEFAULT_PALETTE[i][0] = 0;
            DEFAULT_PALETTE[i][1] = 0xFFFFFFFF;
            DEFAULT_PALETTE[i][2] = 0xFF7F7F7F;
            DEFAULT_PALETTE[i][3] = 0xFF000000;
        }
    }

    public static int[][] loadPaletteFromStream(InputStream in) throws IOException {
        int[][] result = new int[256][4];

        BufferedImage img = ImageIO.read(in);
        int[] data = img.getRGB(0, 0, 32, 32, null, 0, 32);

        for (int i=0; i<data.length; i++) {
            int blockId = ((i >> 1) & 0xF) | ((i >> 2) & 0xF0);
            int paletteId = (i & 1) | ((i >> 4) & 2);
            result[blockId][paletteId] = data[i];
        }

        return result;
    }

    public static int[][] loadPaletteFromFile(File file) throws IOException {
        return loadPaletteFromStream(new FileInputStream(file));
    }
}
