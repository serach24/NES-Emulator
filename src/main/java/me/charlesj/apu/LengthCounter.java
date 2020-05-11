package me.charlesj.apu;

/**
 * Length counter unit.
 * 2020/2/3.
 */
public class LengthCounter {

    private static final int[] LENGTH_COUNTER_TABLE = {
            10,254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
            12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
    };

    private boolean halt;
    private int lengthCounter;
    private boolean enabled;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.lengthCounter = 0;
        }
    }

    public void setHalt(boolean halt) {
        this.halt = halt;
    }

    public void setRegister(int value) {
        if (enabled) {
            lengthCounter = LENGTH_COUNTER_TABLE[(value & 0xF8) >> 3];
        }
    }

    public void clock() {
        if (!halt && lengthCounter != 0) {
            lengthCounter--;
        }
    }

    public int getLengthCounter() {
        return lengthCounter;
    }
}
