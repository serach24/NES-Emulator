package me.charlesj.util.patternconvert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 2020/2/16.
 */
public class PatternConverter {
    public static void convert(byte[] pattern, int offset, int[][] palette, String outputFile) throws IOException {
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        int[] buffer = new int[256];
        for (int i=0; i<0x1000; i+=16) {
            int blockId = (i >> 4) & 0xFF;
            int x = ((i >> 4) & 0xF) << 3;
            int y = ((i >> 8) & 0xF) << 3;
            for (int j=i, c=0; j<i+8; j++) {
                int patternLow = pattern[j+offset];
                int patternHigh = pattern[j+8+offset];
                for (int k=0; k<8; k++, c++) {
                    int val = ((patternLow >> 7) & 1) | ((patternHigh >> 6) & 2);
                    buffer[c] = palette[blockId][val];
                    patternLow <<= 1;
                    patternHigh <<= 1;
                }
            }
            img.setRGB(x, y, 8, 8, buffer, 0, 8);
        }

        ImageIO.write(img, "png", new File(outputFile));
    }

    public static void main(String[] args) throws IOException {
        int[][] palette = PaletteLoader.loadPaletteFromFile(new File("small_palette.png"));
        byte[] data = PatternBuilder.build("small.png", palette, "out.chr");
        convert(data, 0, PaletteLoader.DEFAULT_PALETTE, "out.png");
    }
}
