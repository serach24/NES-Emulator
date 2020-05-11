package me.charlesj.apu;

import me.charlesj.cpu.CPU;

/**
 * Pulse generator.
 * 2020/2/3.
 */
public class Pulse implements SoundGenerator, DividerListener, SweepListener {

    private static final int[][] DUTY_CYCLES = {
            {0, 1, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 0, 0, 0, 0},
            {0, 1, 1, 1, 1, 0, 0, 0},
            {1, 0, 0, 1, 1, 1, 1, 1},
    };

    private Envelop envelop = new Envelop();
    private Divider timer = new DecrementDivider();
    private Sequencer sequencer = new Sequencer(DUTY_CYCLES[0]);
    private Sweep sweep = new Sweep();
    private LengthCounter lengthCounter = new LengthCounter();

    private int currentPeriod;
    private int targetPeriod;

    private boolean evenCycle = false;

    private int id;

    public Pulse(int id) {
        this.id = id;
        timer.setOutputClock(this);
        sweep.setSweepListener(this);
    }

    public void cycle(CPU cpu) {
        evenCycle = !evenCycle;
        if (!evenCycle) {
            return;
        }

        timer.clock();
    }

    public void onClock(Divider divider) {
        sequencer.step();
    }

    public void onSweep() {
        targetPeriod = sweep.calculateTargetPeriod(currentPeriod, id == 1);
        if (targetPeriod < 0) {
            targetPeriod = 0;
        }
        if (targetPeriod < 0x800) {
            currentPeriod = targetPeriod;
        }
        if (sweep.getShiftCount() != 0) {
            timer.setPeriod(currentPeriod);
        }
    }

    public void clockLengthCounterAndSweep() {
        lengthCounter.clock();
        sweep.clock();
    }

    public void clockEnvelopAndLinearCounter() {
        envelop.clock();
    }

    public int output() {
        if (sequencer.output() == 0 || lengthCounter.getLengthCounter() == 0 || targetPeriod > 0x7FF || timer.getValue() < 8) {
            return 0;
        } else {
            return envelop.output();
        }
    }

    public void setEnabled(boolean enabled) {
        lengthCounter.setEnabled(enabled);
    }

    public void setRegister(int index, int value) {
        switch (index) {
            case 0:
                envelop.setRegister(value);
                lengthCounter.setHalt(envelop.getLoopFlag());
                sequencer.setSequence(DUTY_CYCLES[(value >> 6) & 3]);
                break;
            case 1:
                sweep.setRegister(value);
                break;
            case 2:
                currentPeriod = (currentPeriod & ~0xFF) | value;
                targetPeriod = sweep.calculateTargetPeriod(currentPeriod, id == 1);
                timer.setPeriod(currentPeriod);
                break;
            case 3:
                currentPeriod = (currentPeriod & 0xFF) | ((value & 7) << 8);
                targetPeriod = sweep.calculateTargetPeriod(currentPeriod, id == 1);
                timer.setPeriod(currentPeriod);
                lengthCounter.setRegister(value);
                sequencer.reset();
                envelop.setStartFlag();
                break;
        }
    }

    public boolean isActive() {
        return lengthCounter.getLengthCounter() > 0;
    }
}
