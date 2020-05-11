package me.charlesj.speaker;

/**
 * Output wave from APU signals.
 * 2020/2/3.
 */
public interface Speaker {
    /**
     * @param level from 0 ~ 255
     */
    void set(int level);
    byte[] output();
    void reset();
}
