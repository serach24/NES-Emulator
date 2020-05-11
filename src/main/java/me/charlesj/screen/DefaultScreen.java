package me.charlesj.screen;

import me.charlesj.ppu.PPU;
import me.charlesj.util.ColorConverter;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Maps the color using standard color map. Convert it to BufferedImage.
 * 2020/1/25.
 */
public class DefaultScreen implements Screen {
    private BufferedImage image = new BufferedImage(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private int[] imageBuffer = new int[PPU.SCREEN_WIDTH * PPU.SCREEN_HEIGHT];
    private byte[] colorBuffer = new byte[PPU.SCREEN_WIDTH * PPU.SCREEN_HEIGHT];

    public DefaultScreen() {
        Arrays.fill(colorBuffer, (byte) 0x3F);
    }

    public void set(int x, int y, int color) {
        colorBuffer[PPU.SCREEN_WIDTH * y + x] = (byte) color;
    }

    public BufferedImage show() {
        for (int i=0; i<colorBuffer.length; i++) {
            imageBuffer[i] = ColorConverter.COLOR_MAP[colorBuffer[i] & 0x3F];
        }
        image.setRGB(0, 0, PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT, imageBuffer, 0, PPU.SCREEN_WIDTH);
        return image;
    }
}
