package me.charlesj.apu;

import me.charlesj.cpu.CPU;

/**
 * Noise unit.
 * 2020/2/5.
 */
public class Noise implements SoundGenerator, DividerListener {

    private static final int[] PERIOD_TABLE = {4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068};

    private Envelop envelop = new Envelop();
    private Divider timer = new DecrementDivider();
    private LengthCounter lengthCounter = new LengthCounter();

    private boolean mode;
    private int period;
    private int feedbackRegister = 1;

    private boolean evenCycle = false;

    public Noise() {
        timer.setOutputClock(this);
    }

    public void cycle(CPU cpu) {
        evenCycle = !evenCycle;
        if (!evenCycle) {
            return;
        }

        timer.clock();
    }

    public void onClock(Divider divider) {
        int feedback = feedbackRegister;
        if (mode) {
            feedback ^= feedbackRegister >> 6;
        } else {
            feedback ^= feedbackRegister >> 1;
        }
        feedbackRegister >>= 1;
        feedbackRegister |= ((feedback & 1) << 14);
    }

    public void clockLengthCounterAndSweep() {
        lengthCounter.clock();
    }

    public void clockEnvelopAndLinearCounter() {
        envelop.clock();
    }

    public void setEnabled(boolean enabled) {
        lengthCounter.setEnabled(enabled);
    }

    public void setRegister(int index, int value) {
        switch (index) {
            case 0:
                envelop.setRegister(value);
                lengthCounter.setHalt(envelop.getLoopFlag());
                break;
            case 2:
                mode = (value & 0x80) != 0;
                period = PERIOD_TABLE[value & 0xF];
                timer.setPeriod(period);
                break;
            case 3:
                lengthCounter.setRegister(value);
                envelop.setStartFlag();
                break;
            default:
                break;
        }
    }

    public int output() {
        if ((feedbackRegister & 1) != 0 || lengthCounter.getLengthCounter() == 0){
            return 0;
        } else {
            return envelop.output();
        }
    }

    public boolean isActive() {
        return lengthCounter.getLengthCounter() > 0;
    }
}
