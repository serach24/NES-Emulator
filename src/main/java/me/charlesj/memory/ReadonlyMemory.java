package me.charlesj.memory;

/**
 * Readonly memory to emulate ROM.
 * 2020/1/26.
 */
public class ReadonlyMemory extends DefaultMemory {

    public ReadonlyMemory(int size) {
        super(size);
    }

    public ReadonlyMemory(byte[] data) {
        super(data);
    }

    public ReadonlyMemory(byte[] data, int offset, int size) {
        super(data, offset, size);
    }

    @Override
    public void setByte(int address, int value) {
        throw new UnsupportedOperationException("ReadonlyMemory cannot setByte");
    }
}
