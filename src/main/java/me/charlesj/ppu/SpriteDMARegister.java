package me.charlesj.ppu;

import me.charlesj.memory.Memory;
import me.charlesj.cpu.CPU;

/**
 * Mapped to $4014 to transfer CPU memory to PPU SPR-RAM.
 * 2020/1/24.
 */
public class SpriteDMARegister implements Memory {

    private final CPU cpu;
    private final Memory cpuMemory;
    private final Memory sprRam;

    public SpriteDMARegister(Memory cpuMemory, Memory sprRam, CPU cpu) {
        this.cpu = cpu;
        this.cpuMemory = cpuMemory;
        this.sprRam = sprRam;
    }

    public int getSize() {
        return 1;
    }

    public int getByte(int address) {
        return 0;
    }

    public void setByte(int address, int value) {
        cpu.increaseCycle(513);
        value = (value & 0xFF) << 8;
        for (int i=0; i<256; i++) {
            sprRam.setByte(i, cpuMemory.getByte(value + i));
        }
    }
}
