package me.charlesj.input;

/**
 * Standard 4-button 1-pad controllers.
 * controllerId = 1 for 1P, = 2 for 2P.
 * 2020/1/28.
 */
public class StandardControllers extends Input {

    public static final int KEY_A = 0;
    public static final int KEY_B = 1;
    public static final int KEY_SELECT = 2;
    public static final int KEY_START = 3;
    public static final int KEY_UP = 4;
    public static final int KEY_DOWN = 5;
    public static final int KEY_LEFT = 6;
    public static final int KEY_RIGHT = 7;

    private boolean strobe;

    private int[] outputStatus = new int[2];
    private int[] pressStatus = new int[2];

    @Override
    protected void writeRegister(int value) {
        strobe = (value & 1) != 0;
        outputStatus[0] = 0;
        outputStatus[1] = 0;
    }

    @Override
    protected int get(int i) {
        int result = (pressStatus[i] >> outputStatus[i]) & 1;
        if (!strobe) {
            outputStatus[i] = (outputStatus[i] + 1) & 0x7;
        }
        return result;
    }

    public void press(int controllerId, int key) {
        pressStatus[controllerId] |= (1 << key);
    }

    public void release(int controllerId, int key) {
        pressStatus[controllerId] &= ~(1 << key);
    }

    public boolean isStrobe() {
        return strobe;
    }
}
