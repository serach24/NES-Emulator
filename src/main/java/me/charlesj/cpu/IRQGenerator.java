package me.charlesj.cpu;

/**
 * 2020/2/14.
 */
public interface IRQGenerator {
    /**
     * @return false for high, true for low. Low to generate IRQ.
     */
    boolean getIRQLevel();
}
