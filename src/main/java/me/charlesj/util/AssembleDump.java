package me.charlesj.util;

import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.ReadonlyMemory;
import me.charlesj.nesloader.FileNesLoader;
import me.charlesj.nesloader.NesLoader;

import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Reads assembly in NES file and output them as human-friendly style.
 * This file is for test.
 * 2020/1/27.
 */
public class AssembleDump {
    public static void main(String[] args) throws IOException {
        NesLoader loader = new FileNesLoader("game2.nes");
        CompositeMemory memory = new CompositeMemory(0x10000);
        memory.setMemory(0x8000, new ReadonlyMemory(loader.getPRGPage(0)));
        memory.setMemory(0xC000, new ReadonlyMemory(loader.getPRGPage(1)));

        List<Range> ranges = new ArrayList<Range>();
        Queue<Integer> startAddress = new LinkedList<Integer>();
        startAddress.add(memory.getByte(0xFFFA) | (memory.getByte(0xFFFB) << 8));
        startAddress.add(memory.getByte(0xFFFC) | (memory.getByte(0xFFFD) << 8));
        startAddress.add(memory.getByte(0xFFFE) | (memory.getByte(0xFFFF) << 8));

        o: while (!startAddress.isEmpty()) {
            int start = startAddress.poll();

            for (Range range : ranges) {
                if (range.contains(start)) {
                    continue o;
                }
            }

            System.out.printf("Start: %04X\n", start);

            AssembleTexter texter = new AssembleTexter(new MemoryInputStream(memory, start), start);
            while (true) {
                try {
                    texter.getNextInstruction();
                } catch (EOFException e) {
                    break;
                }
                int opcode = texter.getOpcode();
                if (opcode == 0x4C || opcode == 0x20) {
                    int address = (texter.getArg2() << 8) | texter.getArg1();
                    startAddress.add(address);
                }
                if ((opcode & 0x1F) == 0x10) {
                    int address = texter.getCurrentOffset() + (byte) texter.getArg1();
                    startAddress.add(address);
                }
                if (opcode == 0x40 || opcode == 0x60 || opcode == 0x4C || opcode == 0x6C) {
                    ranges.add(new Range(start, texter.getCurrentOffset()));
                    break;
                }
            }
        }

        ranges.add(new Range(0x10000, 0x10000));

        FileOutputStream out = new FileOutputStream("code.txt");
        PrintStream stream = new PrintStream(out);

        ranges.sort(null);
        int i = 0;
        int v = 0x8000;
        while (v < 0x10000) {
            Range r = ranges.get(i);

            if (v < r.from) {
                v = v & ~0xF;
                for (; v <= r.from && v < 0x10000; v += 16) {
                    stream.printf("%04X: %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X\n",
                            v,
                            memory.getByte(v),
                            memory.getByte(v + 1),
                            memory.getByte(v + 2),
                            memory.getByte(v + 3),
                            memory.getByte(v + 4),
                            memory.getByte(v + 5),
                            memory.getByte(v + 6),
                            memory.getByte(v + 7),
                            memory.getByte(v + 8),
                            memory.getByte(v + 8 + 1),
                            memory.getByte(v + 8 + 2),
                            memory.getByte(v + 8 + 3),
                            memory.getByte(v + 8 + 4),
                            memory.getByte(v + 8 + 5),
                            memory.getByte(v + 8 + 6),
                            memory.getByte(v + 8 + 7)
                    );
                }
                stream.println();
            }

            v = r.from;
            if (v >= 0x10000) {
                break;
            }

            AssembleTexter texter = new AssembleTexter(new MemoryInputStream(memory, v), v);
            while (texter.getCurrentOffset() < r.to) {
                stream.println(texter.getNextInstruction());
            }
            stream.println();

            v = r.to;
            i++;
        }

        out.close();
    }

    static class Range implements Comparable<Range> {
        int from;
        int to;
        public Range(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public int compareTo(Range o) {
            return from - o.from;
        }

        public boolean contains(int value) {
            return from <= value && value < to;
        }
    }

    private void dumpPage(byte[] page, int offset) {
        for (int i = 0; i < 16; i++) {
            if (i % 16 == 0) {
                System.out.printf("     | ");
            }
            System.out.printf("%2X ", i);
        }
        System.out.println();
        System.out.print("-----+-");
        for (int i = 0; i < 16; i++) {
            System.out.print("---");
        }
        System.out.println();
        for (int i = 0; i < page.length; i++) {
            if (i % 16 == 0) {
                System.out.printf("%04X | ", i + offset);
            }
            System.out.printf("%02X ", page[i]);
            if (i % 16 == 15) {
                System.out.println();
            }
        }
    }
}
