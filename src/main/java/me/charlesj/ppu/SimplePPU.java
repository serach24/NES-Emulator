package me.charlesj.ppu;

import me.charlesj.cpu.CPU;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.DefaultMemory;
import me.charlesj.memory.Memory;
import me.charlesj.memory.MirrorMemory;
import me.charlesj.screen.Screen;

import java.util.Arrays;

/**
 * Simple PPU implementation.
 * 2020/1/23.
 */
public class SimplePPU implements PPU {

    private PPURegister r = new PPURegister(this);

    private CompositeMemory m = new CompositeMemory(0x10000);

    // sprRam
    private Memory sm = new DefaultMemory(256);

    private Memory[] splitNameTables = new Memory[4];
    private CompositeMemory nameTables = new CompositeMemory(0x1000);
    private Memory patterns = new DefaultMemory(0x2000);
    private Memory palettes = new Palettes();

    private int scanline = 261;
    private int cycle = 0;

    /**
     * byte 0 = 1: is sprite 0, 0: not
     * byte 1 = 1: behind, 0: not
     */
    private byte[][] spriteAttributes = new byte[SCREEN_HEIGHT][SCREEN_WIDTH];
    private byte[][] spriteBuffer = new byte[SCREEN_HEIGHT][SCREEN_WIDTH];
    private byte[][] buffer = new byte[SCREEN_HEIGHT][SCREEN_WIDTH];

    private int[] scanlineSpriteCount = new int[SCREEN_HEIGHT];

    private int sprite0HitCycle = -1;

    private int renderY = 0;
    private int renderX = 0;

    private int mirroringType = -1;

    public SimplePPU() {
        for (int i=0; i<4; i++) {
            splitNameTables[i] = new DefaultMemory(0x400);
            nameTables.setMemory(0x400 * i, splitNameTables[i]);
        }
        resetVRamMemory();
    }

    private void resetVRamMemory() {
        m.setMemory(0, patterns);
        m.setMemory(0x2000, nameTables);
        m.setMemory(0x3000, new MirrorMemory(nameTables, 0xF00));
        m.setMemory(0x3F00, palettes);
        m.setMemory(0x3F20, new MirrorMemory(palettes, 0xE0));
        m.setMemory(0x4000, new MirrorMemory(m, 0xC000));
    }

    public void setCHRMemory(Memory chrRom) {
        patterns = chrRom;
        resetVRamMemory();
    }

    public void setMirroringType(int mirroringType) {
        if (mirroringType == this.mirroringType) {
            return;
        }
        switch (mirroringType) {
            case HORIZONTAL_MIRRORING: {
                Memory leftTop = splitNameTables[0];
                Memory leftBottom = splitNameTables[1];
                nameTables.setMemory(0, leftTop);
                nameTables.setMemory(0x400, leftTop);
                nameTables.setMemory(0x800, leftBottom);
                nameTables.setMemory(0xC00, leftBottom);
                break;
            }
            case VERTICAL_MIRRORING: {
                Memory leftTop = splitNameTables[0];
                Memory rightTop = splitNameTables[1];
                nameTables.setMemory(0, leftTop);
                nameTables.setMemory(0x400, rightTop);
                nameTables.setMemory(0x800, leftTop);
                nameTables.setMemory(0xC00, rightTop);
                break;
            }
            case FOUR_SCREEN_MIRRORING:
                for (int i=0; i<4; i++) {
                    nameTables.setMemory(0x400 * i, splitNameTables[i]);
                }
                break;
            case ONE_SCREEN_MIRRORING:
                for (int i=0; i<4; i++) {
                    nameTables.setMemory(0x400 * i, splitNameTables[1]);
                }
                break;
            default:
                throw new IllegalArgumentException("Must be a mirroring type");
        }
        this.mirroringType = mirroringType;
        resetVRamMemory();
    }

    public PPURegister getRegister() {
        return r;
    }

    public Memory getSprRam() {
        return sm;
    }

    public void writeRegister(int index, int value) {
        switch (index) {
            case 3:
                r.setOAMData(sm.getByte(value & 0xFF));
                break;
            case 4:
                sm.setByte(r.getOAMAddress(), value);
                r.increaseOAMAddress();
                r.setOAMData(sm.getByte(r.getOAMAddress() & 0xFF));
                break;
            case 7:
                m.setByte(r.getPPUAddress(), value);
                r.increasePPUAddress();
                r.setPPUData(m.getByte(r.getPPUAddress()));
                return;
            default:
                break;
        }
    }

    public int readRegister(int address) {
        int result = r.getData(address);
        switch (address) {
            case 2:
                r.clearVerticalBlank();
                break;
            case 7:
                if (r.getPPUAddress() >= 0x3F00) {
                    result = m.getByte(r.getPPUAddress());
                }
                savePPUDataToRegister();
                r.increasePPUAddress();
                break;
            default:
                break;
        }
        return result;
    }

    public void savePPUDataToRegister() {
        r.setPPUData(m.getByte(r.getPPUAddress()));
    }

    public void cycle(Screen screen, CPU cpu) {
        if (scanline == 261) {  //Pre-render line
            if (cycle == 1) {
                r.clearVerticalBlank();
                r.clearSprite0Hit();
                r.clearSpriteOverflow();
                renderY = -1;
            } else if (cycle >= 280 && cycle <= 304) {
                if (r.isRenderingEnabled()) {
                    r.updateTToV(0x7BE0);   //update y
                }
                preRenderSprites();
            }
        } else if (scanline == 241) {
            if (cycle == 1) {
                r.setVerticalBlank();
                if (r.getGenerateNMI()) {
                    cpu.nmi();
                }
            }
        }

        if (scanline == 261 || scanline < SCREEN_HEIGHT) {
            if (((cycle > 0 && cycle <= SCREEN_WIDTH) || cycle > 320) && (cycle & 7) == 0) {
                if (r.isRenderingEnabled()) {
                    renderTileLine();
                    if (cycle == 256) {
                        r.increaseYScrollBy1();
                    } else {
                        r.increaseXScrollBy8();
                    }
                }
            }
            if (cycle == 257) {
                if (r.isRenderingEnabled()) {
                    r.updateTToV(0x41F);  //update x
                    renderX = 0;
                    renderY++;
                    sprite0HitCycle = -1;
                }
            }
        }

        if (scanline >= 0 && scanline < SCREEN_HEIGHT) {
            if (cycle > 0 && cycle <= SCREEN_WIDTH) {
                if (cycle == 1) {
                    cycle = 1;
                }
                if (r.isRenderingEnabled()) {
                    screen.set(cycle - 1, scanline, buffer[scanline][cycle - 1]);
                } else {
                    screen.set(cycle - 1, scanline, palettes.getByte(0));
                }
                if (r.showBackground() && r.showSprites() && sprite0HitCycle == cycle) {
                    r.setSprite0Hit();
                }
            }
        }

        cycle++;
        if (cycle == 341) {
            cycle = 0;
            scanline++;
            if (scanline == 262) {
                scanline = 0;
            }
        }
    }

    public void powerUp() {
        reset();
    }

    public void reset() {
        r.reset();
        scanline = 261;
        cycle = 0;
    }

    public boolean inVerticalBlank() {
        return scanline >= SCREEN_HEIGHT;
    }

    private void renderTileLine() {
        if (renderY < 0 || renderY >= SCREEN_HEIGHT) {
            return;
        }

        byte[] bufferLine = buffer[renderY];
        int x = renderX - r.getFineXScroll();

        for (int i = x; i < x + 8; i++) {
            if (i < 0 || i >= SCREEN_WIDTH) {
                continue;
            }
            bufferLine[i] = -1;
        }

        if (!r.showBackground()) {
            return;
        }

        int attribute = m.getByte(r.getAttributeAddress());
        int palette;
        boolean left = r.isPaletteLeft();
        boolean top = r.isPaletteTop();
        if (top && left) {
            palette = attribute & 3;
        } else if (top) {
            palette = (attribute >> 2) & 3;
        } else if (left) {
            palette = (attribute >> 4) & 3;
        } else {
            palette = (attribute >> 6) & 3;
        }

        int y = r.getFineYScroll();
        int pattern = m.getByte(r.getTileAddress());
        int patternAddress = r.getBackgroundPatternTableAddress() + (pattern << 4);
        int patternLow = m.getByte(patternAddress + y);
        int patternHigh = m.getByte(patternAddress + y + 8);
        int paletteAddress = palette << 2;

        byte[] spriteBufferLine = spriteBuffer[renderY];
        byte[] spriteAttributeLine = spriteAttributes[renderY];

        byte backdropColor = (byte) palettes.getByte(0);

        for (int i = x; i < x + 8; i++) {
            if (i < SCREEN_WIDTH && (i >= 8 || (r.showLeftmost8PixelsBackground() && i >= 0))) {
                int v = ((patternHigh >> 6) & 2) | ((patternLow >> 7) & 1);
                if (v != 0) {
                    bufferLine[i] = (byte) palettes.getByte(paletteAddress | v);
                }
                if (r.showSprites() && spriteBufferLine[i] != -1) {
                    int attr = spriteAttributeLine[i];
                    if (sprite0HitCycle == -1 && (attr & 1) != 0 && bufferLine[i] != -1) {
                        sprite0HitCycle = i + 1;
                    }
                    if (bufferLine[i] == -1 || (attr & 2) == 0) {
                        bufferLine[i] = spriteBufferLine[i];
                    }
                }
                if (bufferLine[i] == -1) {
                    bufferLine[i] = backdropColor;
                }
            }
            patternHigh <<= 1;
            patternLow <<= 1;
        }

        renderX += 8;
    }

    private void preRenderSprites() {
        for (int j=0; j<SCREEN_HEIGHT; j++) {
            for (int i=0; i<SCREEN_WIDTH; i++) {
                spriteBuffer[j][i] = -1;
                spriteAttributes[j][i] = 0;
            }
        }

        if (!r.showSprites()) {
            return;
        }

        int patternTableAddress = r.getSpritePatternTableAddress();
        boolean is8x16 = r.is8x16();

        Arrays.fill(scanlineSpriteCount, 0);

        for (int i=256-4; i>=0; i-=4) {
            int id = i >> 2;
            int y = sm.getByte(i) + 1;
            int pattern = sm.getByte(i + 1);
            int attribute = sm.getByte(i + 2);
            int x = sm.getByte(i + 3);
            int palette = attribute & 3;
            boolean behindBackground = (attribute & 0x20) != 0;
            boolean flipHorizontally = (attribute & 0x40) != 0;
            boolean flipVertically = (attribute & 0x80) != 0;

            if (y >= 240) {
                continue;
            }

            if (is8x16) {
                for (int j=0; j<16; j++) {
                    if (y+j >= 0 && y+j < SCREEN_HEIGHT) {
                        scanlineSpriteCount[y+j]++;
                    }
                }

                patternTableAddress = (pattern & 1) << 12;
                pattern &= ~1;

                if (flipVertically) {
                    preRenderSprite(id, x, y, patternTableAddress + ((pattern + 1) << 4), 0x10 + (palette << 2), behindBackground, flipHorizontally, true);
                    preRenderSprite(id, x, y + 8, patternTableAddress + (pattern << 4), 0x10 + (palette << 2), behindBackground, flipHorizontally, true);
                } else {
                    preRenderSprite(id, x, y, patternTableAddress + (pattern << 4), 0x10 + (palette << 2), behindBackground, flipHorizontally, false);
                    preRenderSprite(id, x, y + 8, patternTableAddress + ((pattern + 1) << 4), 0x10 + (palette << 2), behindBackground, flipHorizontally, false);
                }
            } else {
                for (int j=0; j<8; j++) {
                    if (y+j >= 0 && y+j < SCREEN_HEIGHT) {
                        scanlineSpriteCount[y+j]++;
                    }
                }

                if (i == 0) {
                    preRenderSprite(id, x, y, patternTableAddress + (pattern << 4), 0x10 + (palette << 2), behindBackground, flipHorizontally, flipVertically);
                } else {
                    preRenderSprite(id, x, y, patternTableAddress + (pattern << 4), 0x10 + (palette << 2), behindBackground, flipHorizontally, flipVertically);
                }
            }
        }
    }

    private void preRenderSprite(int id, int x, int y, int patternAddress, int paletteAddress, boolean behind, boolean flipHorizontally, boolean flipVertically) {
        if (x <= -8 || y <= -8 || x >= SCREEN_WIDTH || y >= SCREEN_HEIGHT) {
            return;
        }
        for (int j = y, jj = flipVertically ? 7 : 0; j < y + 8; j++, jj += (flipVertically ? -1 : 1)) {
            if (j < 0 || j >= SCREEN_HEIGHT) {
                continue;
            }
            byte patternLow = (byte) patterns.getByte(patternAddress + jj);
            byte patternHigh = (byte) patterns.getByte(patternAddress + jj + 8);
            for (int i = x; i < x + 8; i++) {
                if (i < SCREEN_WIDTH && (i >= 8 || (r.showLeftmost8PixelsSprites() && i >= 0))) {
                    int v;
                    if (flipHorizontally) {
                        v = ((patternHigh << 1) & 2) | (patternLow & 1);
                    } else {
                        v = ((patternHigh >> 6) & 2) | ((patternLow >> 7) & 1);
                    }
                    if (v != 0) {
                        if (spriteBuffer[j][i] == -1 ||
                                !((behind && (spriteAttributes[j][i] & 2) == 0))) {
                            spriteBuffer[j][i] = (byte) palettes.getByte(paletteAddress | v);
                            spriteAttributes[j][i] = (byte) ((behind ? 2 : 0) | (spriteAttributes[j][i] & 1));
                        }
                        if (id == 0) {
                            spriteAttributes[j][i] |= 1;
                        }
                    }
                }
                if (flipHorizontally) {
                    patternHigh >>= 1;
                    patternLow >>= 1;
                } else {
                    patternHigh <<= 1;
                    patternLow <<= 1;
                }
            }
        }
    }

    public int getScanline() {
        return scanline;
    }

    public int getCycle() {
        return cycle;
    }

    public Memory getMemory() {
        return m;
    }

    private class Palettes extends DefaultMemory {
        public Palettes() {
            super(0x20);
        }

        @Override
        public int getByte(int address) {
            int result = super.getByte(address);
            if (r.isGreyScale()) {
                return result & 0x30;
            }
            return result;
        }

        @Override
        public void setByte(int address, int value) {
            super.setByte(address, value);
            if (address == 0x10) {
                super.setByte(0, value);
            }
        }
    }
}
