package me.charlesj.apu;

import me.charlesj.cpu.CPU;
import me.charlesj.cpu.IRQGenerator;
import me.charlesj.speaker.Speaker;

/**
 * APU interface.
 * 2020/1/22.
 */
public interface APU extends IRQGenerator {

    APURegister getRegister();

    void writeRegister(int index, int value);
    int readRegister(int index);

    void cycle(Speaker speaker, CPU cpu);
    void powerUp();
    void reset();
}
