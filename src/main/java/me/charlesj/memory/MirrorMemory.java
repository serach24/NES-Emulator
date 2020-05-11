package me.charlesj.memory;

/**
 * Access to this memory is equivalent to access another memory.
 * 2020/1/23.
 */
public class MirrorMemory implements Memory {

    private final Memory source;
    private final int offset;
    private final int size;

    public MirrorMemory(Memory source, int offset, int size) {
        this.source = source;
        this.offset = offset;
        this.size = size;
    }

    public MirrorMemory(Memory source, int size) {
        this(source, 0, size);
    }

    public int getSize() {
        return size;
    }

    public int getByte(int address) {
        return source.getByte((address + offset) % source.getSize());
    }

    public void setByte(int address, int value) {
        source.setByte((address + offset) % source.getSize(), value);
    }
}
