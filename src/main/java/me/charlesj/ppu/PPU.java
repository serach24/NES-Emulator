package me.charlesj.ppu;

import me.charlesj.memory.Memory;
import me.charlesj.screen.Screen;
import me.charlesj.cpu.CPU;

/**
 * PPU interface.
 * 2020/1/22.
 */
public interface PPU {

    int SCREEN_WIDTH = 256;
    int SCREEN_HEIGHT = 240;

    int VERTICAL_MIRRORING = 0;
    int HORIZONTAL_MIRRORING = 1;
    int ONE_SCREEN_MIRRORING = 2;
    int FOUR_SCREEN_MIRRORING = 3;

    void setCHRMemory(Memory chrRom);
    void setMirroringType(int mirroringType);

    PPURegister getRegister();
    Memory getSprRam();

    void writeRegister(int index, int value);
    int readRegister(int index);

    void cycle(Screen screen, CPU cpu);
    void powerUp();
    void reset();

    boolean inVerticalBlank();

    int getScanline();
    int getCycle();
}
