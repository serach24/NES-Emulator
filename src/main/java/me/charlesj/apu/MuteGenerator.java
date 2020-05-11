package me.charlesj.apu;

import me.charlesj.cpu.CPU;

/**
 * Mute generator. Only for test.
 * 2020/2/5.
 */
public class MuteGenerator implements SoundGenerator {
    public void cycle(CPU cpu) {

    }

    public void setEnabled(boolean enabled) {

    }

    public void setRegister(int index, int value) {

    }

    public int output() {
        return 0;
    }

    public boolean isActive() {
        return false;
    }

    public void clockLengthCounterAndSweep() {

    }

    public void clockEnvelopAndLinearCounter() {

    }
}
