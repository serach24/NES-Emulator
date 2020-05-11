package me.charlesj.memory;

/**
 * Memory with write protect.
 * 2020/2/14.
 */
public class WriteProtectMemory extends DefaultMemory {
    private boolean enableWrite = true;

    public WriteProtectMemory(int size) {
        super(size);
    }

    public WriteProtectMemory(byte[] data) {
        super(data);
    }

    public WriteProtectMemory(byte[] data, int offset, int size) {
        super(data, offset, size);
    }

    @Override
    public void setByte(int address, int value) {
        if (enableWrite) {
            super.setByte(address, value);
        }
    }

    public void setEnableWrite(boolean enableWrite) {
        this.enableWrite = enableWrite;
    }
}
