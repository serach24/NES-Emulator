package me.charlesj.memory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Combine multiple Memory into one. Used to map CPU or PPU memory.
 * 2020/1/23.
 */
public class CompositeMemory implements Memory {

    private int size;
    private List<OffsetMemoryPair> memories = new ArrayList<OffsetMemoryPair>();
    private int[] offsets = new int[0];

    public CompositeMemory(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public int getByte(int address) {
        OffsetMemoryPair pair = findMemoryPair(address);
        assert pair != null;
        return pair.memory.getByte(address - pair.offset);
    }

    public void setByte(int address, int value) {
        OffsetMemoryPair pair = findMemoryPair(address);
        assert pair != null;
        pair.memory.setByte(address - pair.offset, value);
    }

    public void setMemory(int offset, Memory memory) {
        OffsetMemoryPair find = null;
        for (OffsetMemoryPair pair : memories) {
            if (pair.offset == offset) {
                find = pair;
                break;
            }
        }

        if (find != null) {
            find.memory = memory;
        } else {
            memories.add(new OffsetMemoryPair(offset, memory));
            memories.sort(null);
            offsets = new int[memories.size()];
            for (int i=0; i<offsets.length; i++) {
                offsets[i] = memories.get(i).offset;
            }
        }
    }

    private OffsetMemoryPair findMemoryPair(int address) {
        int index = Arrays.binarySearch(offsets, address);
        if (index == -1) {
            return null;
        } else if (index < 0) {
            index = -index - 2;
        }
        return memories.get(index);
    }

    class OffsetMemoryPair implements Comparable<OffsetMemoryPair> {
        int offset;
        Memory memory;
        public OffsetMemoryPair(int offset, Memory memory) {
            this.offset = offset;
            this.memory = memory;
        }
        public int compareTo(OffsetMemoryPair o) {
            return offset - o.offset;
        }
    }
}
