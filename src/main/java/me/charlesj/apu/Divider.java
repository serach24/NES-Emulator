package me.charlesj.apu;

/**
 * 2020/2/3.
 */
public interface Divider {
    void setPeriod(int value);
    void setOutputClock(DividerListener outputClock);
    void reset();
    void clock();
    int getValue();
}
