package me.charlesj.apu;

import me.charlesj.cpu.CPU;

/**
 * Triangle generator.
 * 2020/2/4.
 */
public class Triangle implements SoundGenerator, DividerListener {

    private static final int[] TRIANGLE_SEQUENCE = {
            15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
    };

    private Divider timer = new DecrementDivider();
    private LengthCounter lengthCounter = new LengthCounter();
    private Divider linearCounter = new DecrementDivider();
    private boolean reloadFlag;
    private boolean controlFlag;
    private Sequencer sequencer = new Sequencer(TRIANGLE_SEQUENCE);

    private int counterReload;
    private int timerPeriod;

    public Triangle() {
        timer.setOutputClock(this);
    }

    public void cycle(CPU cpu) {
        timer.clock();
    }

    public void clockEnvelopAndLinearCounter() {
        if (reloadFlag) {
            linearCounter.reset();
        } else {
            linearCounter.clock();
        }
        if (!controlFlag) {
            reloadFlag = false;
        }
    }

    public void clockLengthCounterAndSweep() {
        lengthCounter.clock();
    }

    public void onClock(Divider divider) {
        if (linearCounter.getValue() != 0 && lengthCounter.getLengthCounter() != 0) {
            sequencer.step();
        }
    }

    public void setEnabled(boolean enabled) {
        lengthCounter.setEnabled(enabled);
    }

    public void setRegister(int index, int value) {
        switch (index) {
            case 0:
                counterReload = value & 0x7F;
                controlFlag = (value & 0x80) != 0;
                lengthCounter.setHalt(controlFlag);
                linearCounter.setPeriod(counterReload);
                break;
            case 2:
                timerPeriod = (timerPeriod & ~0xFF) | value;
                timer.setPeriod(timerPeriod);
                break;
            case 3:
                timerPeriod = (timerPeriod & 0xFF) | ((value & 7) << 8);
                timer.setPeriod(timerPeriod);
                lengthCounter.setRegister(value);
                reloadFlag = true;
                break;
            default:
                break;
        }
    }

    public int output() {
        if (lengthCounter.getLengthCounter() == 0 || linearCounter.getValue() == 0 || timerPeriod <= 1) {
            return 0;
        } else {
            return sequencer.output();
        }
    }

    public boolean isActive() {
        return lengthCounter.getLengthCounter() > 0 && linearCounter.getValue() > 0;
    }
}
