package me.charlesj.memory;

/**
 * Memory interface.
 * 2020/1/22.
 */
public interface Memory {
    int getSize();
    int getByte(int address);
    void setByte(int address, int value);
}
