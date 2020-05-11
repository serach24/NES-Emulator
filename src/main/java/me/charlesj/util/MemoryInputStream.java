package me.charlesj.util;

import me.charlesj.memory.Memory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Read memory as an InputStream.
 * 2020/1/25.
 */
public class MemoryInputStream extends InputStream {
    private final Memory memory;
    private int offset;

    public MemoryInputStream(Memory memory, int offset) {
        this.memory = memory;
        this.offset = offset;
    }

    @Override
    public int read() throws IOException {
        if (memory.getSize() <= offset) {
            return -1;
        }
        int result = memory.getByte(offset);
        offset++;
        return result;
    }
}
