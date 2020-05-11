package me.charlesj.ppu;

import me.charlesj.memory.DefaultMemory;
import me.charlesj.memory.Memory;

import java.util.Arrays;

/**
 * Register used by PPU. Can be accessed by CPU as memory.
 * 2020/1/24.
 */
public class PPURegister extends DefaultMemory implements Memory {

    private int v;
    private int t;
    private int x;
    private boolean w;

    private final PPU ppu;

    public PPURegister(PPU ppu) {
        super(8);
        this.ppu = ppu;
    }

    @Override
    public void setByte(int address, int value) {
        super.setByte(address, value);
        switch (address) {
            case 0:
                t = (t & ~0xC00) | ((value & 0x3) << 10);
                break;
            case 5:
                if (!w) {
                    t = (t & ~0x1F) | (value >> 3);
                    x = value & 7;
                } else {
                    t = (t & ~0x7000) | ((value & 7) << 12);
                    t = (t & ~0x3E0) | ((value & ~7) << 2);
                }
                w = !w;
                break;
            case 6:
                if (!w) {
                    t = (t & ~0x7F00) | ((value & 0x3F) << 8);
                } else {
                    t = (t & ~0xFF) | (value & 0xFF);
                    v = t;
                }
                w = !w;
                break;
            default:
                break;
        }
        ppu.writeRegister(address, value);
    }

    @Override
    public int getByte(int address) {
        if (address == 2) {
            w = false;
        }
        return ppu.readRegister(address);
    }

    public int getXScroll() {
        return ((v & 0x1F) << 3) | x;
    }

    public int getYScroll() {
        return ((v >> 2) & 0xF8) | ((v >> 12) & 7);
    }

    public void updateTToV(int mask) {
        v = (v & ~mask) | (t & mask);
    }

    public void increaseXScrollBy8() {
        if ((v & 0x001F) == 31) { // if coarse X == 31
            v &= ~0x001F;          // coarse X = 0
            v ^= 0x0400;           // switch horizontal nametable
        } else {
            v += 1;                // increment coarse X
        }
    }

    public void increaseYScrollBy1() {
        if ((v & 0x7000) != 0x7000) {        // if fine Y < 7
            v += 0x1000;                      // increment fine Y
        } else {
            v &= ~0x7000;                     // fine Y = 0
            int y = (v & 0x03E0) >> 5;        // let y = coarse Y
            if (y == 29) {
                y = 0;                          // coarse Y = 0
                v ^= 0x0800;                    // switch vertical nametable
            } else if (y == 31) {
                y = 0;                          // coarse Y = 0, nametable not switched
            } else {
                y += 1;                         // increment coarse Y
            }
            v = (v & ~0x03E0) | (y << 5);     // put coarse Y back into v
        }
    }

    public int getXScrollConsiderBaseNameTableAddress() {
        return getXScroll() + (((v >> 10) & 1) != 0 ? 256 : 0);
    }

    public int getYScrollConsiderBaseNameTableAddress() {
        return getYScroll() + (((v >> 10) & 2) != 0 ? 240 : 0);
    }

    public int getTileAddress() {
        return 0x2000 | (v & 0x0FFF);
    }

    public int getAttributeAddress() {
        return 0x23C0 | (v & 0x0C00) | ((v >> 4) & 0x38) | ((v >> 2) & 0x07);
    }

    public int getFineXScroll() {
        return x;
    }

    public int getFineYScroll() {
        return v >> 12;
    }

    public boolean isPaletteLeft() {
        return ((v >> 1) & 1) == 0;
    }

    public boolean isPaletteTop() {
        return ((v >> 6) & 1) == 0;
    }

    public int getVRAMAddressIncrement() {
        return (data[0] & 4) != 0 ? 32 : 1;
    }

    public int getSpritePatternTableAddress() {
        return (data[0] & 8) != 0 ? 0x1000 : 0;
    }

    public int getBackgroundPatternTableAddress() {
        return (data[0] & 0x10) != 0 ? 0x1000 : 0;
    }

    public boolean is8x16() {
        return (data[0] & 0x20) != 0;
    }

    public boolean getPPUMasterSlaveSelect() {
        return (data[0] & 0x40) != 0;
    }

    public boolean getGenerateNMI() {
        return (data[0] & 0x80) != 0;
    }

    public boolean isGreyScale() {
        return (data[1] & 1) != 0;
    }

    public boolean isEmphasizeRed() {
        return (data[1] & 0x20) != 0;
    }

    public boolean isEmphasizeGreen() {
        return (data[1] & 0x40) != 0;
    }

    public boolean isEmphasizeBlue() {
        return (data[1] & 0x80) != 0;
    }

    public boolean isSprite0Hit() {
        return (data[2] & 0x40) != 0;
    }

    public boolean isVerticalBlank() {
        return (data[2] & 0x80) != 0;
    }

    public int getBackgroundColor() {
        return (data[1] & 1) | ((data[1] & 0xE0) >> 4);
    }

    public boolean showLeftmost8PixelsBackground() {
        return (data[1] & 2) != 0;
    }

    public boolean showLeftmost8PixelsSprites() {
        return (data[1] & 4) != 0;
    }

    public boolean showBackground() {
        return (data[1] & 8) != 0;
    }

    public boolean showSprites() {
        return (data[1] & 0x10) != 0;
    }

    public boolean isRenderingEnabled() {
        return (data[1] & 0x18) != 0;
    }

    public void setVerticalBlank() {
        data[2] |= 0x80;
    }

    public void clearVerticalBlank() {
        data[2] &= ~0x80;
    }

    public void setSprite0Hit() {
        data[2] |= 0x40;
    }

    public void clearSprite0Hit() {
        data[2] &= ~0x40;
    }

    public void setSpriteOverflow() {
        data[2] |= 0x20;
    }

    public void clearSpriteOverflow() {
        data[2] &= ~0x20;
    }

    public void setOAMData(int value) {
        data[4] = (byte) value;
    }

    public int getOAMAddress() {
        return data[3] & 0xFF;
    }

    public void setPPUData(int value) {
        data[7] = (byte) value;
    }

    public int getPPUAddress() {
        return v & 0x3FFF;
    }

    public void reset() {
        Arrays.fill(data, (byte) 0);
        data[2] = (byte) 0xA0;
        w = false;
    }

    public void increaseOAMAddress() {
        data[3] += 1;
    }

    public void increasePPUAddress() {
        v = (v + getVRAMAddressIncrement()) & 0x3FFF;
    }

    public int getData(int index) {
        return data[index] & 0xFF;
    }

    public boolean getW() {
        return w;
    }

    public int getV() {
        return v;
    }
}
