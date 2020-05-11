package me.charlesj.util.patternconvert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 2020/2/16.
 */
public class PatternBuilder {
    public static byte[] build(String patternFile, int[][] palette, String outputFile) throws IOException {
        BufferedImage img = ImageIO.read(new File(patternFile));
        byte[] pattern = new byte[0x1000];

        int[] buffer = new int[256];
        for (int i=0; i<0x1000; i+=16) {
            int blockId = (i >> 4) & 0xFF;
            int x = ((i >> 4) & 0xF) << 3;
            int y = ((i >> 8) & 0xF) << 3;
            img.getRGB(x, y, 8, 8, buffer, 0, 8);
            for (int j=i, c=0; j<i+8; j++) {
                int patternLow = pattern[j];
                int patternHigh = pattern[j+8];
                for (int k=0; k<8; k++, c++) {
                    int val = findNearest(buffer[c], palette[blockId]);
                    patternLow <<= 1;
                    patternHigh <<= 1;
                    patternLow |= val & 1;
                    patternHigh |= (val >> 1) & 1;
                }
                pattern[j] = (byte) patternLow;
                pattern[j+8] = (byte) patternHigh;
            }
        }

        if (outputFile != null) {
            FileOutputStream out = new FileOutputStream(outputFile);
            out.write(pattern);
            out.close();
        }

        return pattern;
    }

    private static int findNearest(int color, int[] palette) {
        int distance = distance(color, palette[0]);
        int result = 0;
        for (int i = 1; i < palette.length; i++) {
            int p = palette[i];
            int d = distance(color, p);
            if (d < distance) {
                distance = d;
                result = i;
            }
        }
        return result;
    }

    private static int distance(int a, int b) {
        int d = 0;
        for (int i=0; i<4; i++) {
            int diff = (a & 0xFF) - (b & 0xFF);
            d += diff * diff;
            a >>= 8;
            b >>= 8;
        }
        return d;
    }
}
