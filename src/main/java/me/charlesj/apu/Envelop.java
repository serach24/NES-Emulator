package me.charlesj.apu;

/**
 * Envelop unit.
 * 2020/2/3.
 */
public class Envelop implements DividerListener {
    private boolean startFlag;
    private Divider divider = new DecrementDivider();
    private int decayLevel;

    private boolean isConstant;
    private int volume;
    private boolean loopFlag;

    public Envelop() {
        divider.setOutputClock(this);
    }

    public void clock() {
        if (!startFlag) {
            divider.clock();
        } else {
            startFlag = false;
            divider.reset();
            decayLevel = 15;
        }
    }

    public void onClock(Divider divider) {
        if (decayLevel > 0) {
            decayLevel--;
        } else if (loopFlag) {
            decayLevel = 15;
        }
    }

    public int output() {
        return isConstant ? volume : decayLevel;
    }

    public void setRegister(int value) {
        isConstant = (value & 0x10) != 0;
        loopFlag = (value & 0x20) != 0;
        volume = value & 0xF;
        divider.setPeriod(volume);
    }

    public void setStartFlag() {
        startFlag = true;
    }

    public boolean getLoopFlag() {
        return loopFlag;
    }
}
