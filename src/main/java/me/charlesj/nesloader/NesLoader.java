package me.charlesj.nesloader;

/**
 * Loader interface.
 * 2020/1/22.
 */
public interface NesLoader {
    int getPRGPageCount();
    int getCHRPageCount();
    byte[] getPRGPage(int index);
    byte[] getCHRPage(int index);
    int getMapper();
    boolean isHorizontalMirroring();
    boolean isVerticalMirroring();
    boolean isSRAMEnabled();
    boolean is512ByteTrainerPresent();
    boolean isFourScreenMirroring();
    byte[] getTrainer();
}
