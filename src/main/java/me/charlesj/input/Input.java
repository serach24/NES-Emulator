package me.charlesj.input;

import me.charlesj.memory.Memory;

/**
 * Game input class. Mapped to $4016-$4017.
 * 2020/1/28.
 */
public abstract class Input implements Memory {
    protected abstract void writeRegister(int value);
    protected abstract int get(int address);

    public int getSize() {
        return 2;
    }

    public int getByte(int address) {
        return get(address);
    }

    public void setByte(int address, int value) {
        if (address == 0) {
            writeRegister(value);
        }
    }
}
