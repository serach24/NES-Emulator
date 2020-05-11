package me.charlesj.cpu;

import me.charlesj.memory.Memory;

/**
 * CPU interface.
 * 2020/1/22.
 */
public interface CPU {
    void setMemory(Memory memory);
    Memory getMemory();
    long execute();
    CPURegister getRegister();
    long getCycle();
    void increaseCycle(int value);
    void reset();
    void powerUp();
    void nmi();
    void addIRQGenerator(IRQGenerator generator);
}
