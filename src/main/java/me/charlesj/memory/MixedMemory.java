package me.charlesj.memory;

/**
 *
 * 2020/1/28.
 */
public class MixedMemory implements Memory {

    public final int size;
    public final Memory read;
    public final Memory write;
    public final int readOffset;
    public final int writeOffset;

    public MixedMemory(int size, Memory read, int readOffset, Memory write, int writeOffset) {
        this.size = size;
        this.read = read;
        this.write = write;
        this.readOffset = readOffset;
        this.writeOffset = writeOffset;
    }

    public int getSize() {
        return size;
    }

    public int getByte(int address) {
        return read.getByte(address + readOffset);
    }

    public void setByte(int address, int value) {
        write.setByte(address + writeOffset, value);
    }
}
