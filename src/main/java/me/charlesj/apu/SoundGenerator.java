package me.charlesj.apu;

import me.charlesj.cpu.CPU;

/**
 * Sound generator interface.
 * 2020/2/3.
 */
public interface SoundGenerator {
    void cycle(CPU cpu);
    void setEnabled(boolean enabled);
    void setRegister(int index, int value);
    int output();
    boolean isActive();
    void clockLengthCounterAndSweep();
    void clockEnvelopAndLinearCounter();
}
