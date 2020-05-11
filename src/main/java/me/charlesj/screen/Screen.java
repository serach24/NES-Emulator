package me.charlesj.screen;

import java.awt.image.BufferedImage;

/**
 * Output PPU-rendered image to Java BufferedImage type.
 * 2020/1/25.
 */
public interface Screen {
    void set(int x, int y, int color);
    BufferedImage show();
}
