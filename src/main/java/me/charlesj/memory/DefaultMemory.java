package me.charlesj.memory;

/**
 * Array based memory implementation.
 * 2020/1/22.
 */
public class DefaultMemory implements Memory {

    protected final byte[] data;
    protected final int size;
    protected final int offset;

    public DefaultMemory(int size) {
        this(new byte[size]);
    }

    public DefaultMemory(byte[] data) {
        this(data, 0, data.length);
    }

    public DefaultMemory(byte[] data, int offset, int size) {
        this.data = data;
        this.offset = offset;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public int getByte(int address) {
        return data[address + offset] & 0xFF;
    }

    public void setByte(int address, int value) {
        data[address + offset] = (byte) value;
    }
}
