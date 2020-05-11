package me.charlesj.apu;

/**
 * Sweep unit of APU.
 * 2020/2/3.
 */
public class Sweep implements DividerListener {
    private Divider divider = new DecrementDivider();
    private boolean reloadFlag;

    private boolean enabled;
    private int period;
    private boolean negate;
    private int shiftCount;

    private SweepListener sweepListener;

    public Sweep() {
        divider.setOutputClock(this);
    }

    public void clock() {
        if (reloadFlag) {
            if (enabled) {
                divider.clock();
            }
            divider.reset();
            reloadFlag = false;
        } else {
            divider.clock();
        }
    }

    public void onClock(Divider divider) {
        if (enabled && sweepListener != null) {
            sweepListener.onSweep();
        }
    }

    public void setRegister(int value) {
        enabled = (value & 0x80) != 0;
        period = (value & 0x70) >> 4;
        negate = (value & 8) != 0;
        shiftCount = value & 7;
        reloadFlag = true;
        divider.setPeriod(period);
    }

    public void setSweepListener(SweepListener sweepListener) {
        this.sweepListener = sweepListener;
    }

    public int calculateTargetPeriod(int currentPeriod, boolean useOnesComponent) {
        int changeAmount = currentPeriod >> shiftCount;
        if (negate) {
            if (useOnesComponent) {
                changeAmount = ~changeAmount;
            } else {
                changeAmount = -changeAmount;
            }
        }
        return currentPeriod + changeAmount;
    }

    public int getShiftCount() {
        return shiftCount;
    }
}
