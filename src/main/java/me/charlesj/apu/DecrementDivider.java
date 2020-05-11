package me.charlesj.apu;

/**
 * 2020/2/3.
 */
public class DecrementDivider implements Divider {
    private int period;
    private int counter;
    private DividerListener outputClock;

    public void setPeriod(int value) {
        period = value;
    }

    public void setOutputClock(DividerListener outputClock) {
        this.outputClock = outputClock;
    }

    public void reset() {
        counter = period;
    }

    public void clock() {
        if (counter == 0) {
            reset();
            triggerOutputClock();
        } else {
            counter--;
        }
    }

    private void triggerOutputClock() {
        if (outputClock != null) {
            outputClock.onClock(this);
        }
    }

    public int getValue() {
        return counter;
    }
}
