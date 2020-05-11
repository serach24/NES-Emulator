package me.charlesj.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Convert opcodes to human-friendly style.
 * 2020/1/24.
 */
public class AssembleTexter {

    private static final Map<String, String> TEXTER = new HashMap<String, String>();

    static {
        TEXTER.put("#i", "%s #%X");
        TEXTER.put("(a)", "%s (%X)");
        TEXTER.put("(d),y", "%s (%X),y");
        TEXTER.put("(d,x)", "%s (%X,x)");
        TEXTER.put("*+d", "%s %X");
        TEXTER.put("a", "%s %X");
        TEXTER.put("a,x", "%s %X,x");
        TEXTER.put("a,y", "%s %X,y");
        TEXTER.put("d", "%s %X");
        TEXTER.put("d,x", "%s %X,x");
        TEXTER.put("d,y", "%s %X,y");
        TEXTER.put("", "%s");
    }

    private InputStream in;
    private int offset;

    private int opcode = 0;
    private int count = 0;
    private int arg1 = 0;
    private int arg2 = 0;

    public AssembleTexter(InputStream in, int start) {
        this.offset = start;
        this.in = in;
    }

    public String getNextInstruction() throws IOException {
        StringBuilder sb = new StringBuilder();

        opcode = in.read();
        if (opcode < 0) {
            throw new EOFException();
        }

        String[] rule = Rules.RULES[opcode];
        String type = rule[1];
        int value;

        if (type.equals("")) {
            value = 0;
            count = 1;
        } else if (type.equals("(a)") || type.startsWith("a")) {
            count = 3;
            arg1 = in.read();
            arg2 = in.read();
            value = (arg2 << 8) | arg1;
            if (value < 0) {
                throw new EOFException();
            }
        } else {
            count = 2;
            arg1 = in.read();
            value = arg1;
            if (value < 0) {
                throw new EOFException();
            }
        }

        if (type.equals("*+d")) {
            value = offset + count + (byte) value;
        }

        sb.append(String.format("%04X: ", offset));

        sb.append(String.format("%-16s", String.format(TEXTER.get(type), rule[0], value)));

        sb.append(String.format("%02X", opcode));
        if (count > 1) {
            sb.append(String.format(" %02X", arg1));
        }
        if (count > 2) {
            sb.append(String.format(" %02X", arg2));
        }

        offset += count;

        return sb.toString();
    }

    public int getCurrentOffset() {
        return offset;
    }

    public void close() throws IOException {
        in.close();
    }

    public int getOpcode() {
        return opcode;
    }

    public int getCount() {
        return count;
    }

    public int getArg1() {
        return arg1;
    }

    public int getArg2() {
        return arg2;
    }
}
