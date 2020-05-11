package me.charlesj.util.assembler;

import me.charlesj.memory.DefaultMemory;
import me.charlesj.memory.Memory;
import me.charlesj.util.AssembleTexter;
import me.charlesj.util.Rules;
import me.charlesj.util.MemoryInputStream;

import java.io.*;
import java.util.*;

/**
 * 6052 Assembler.
 * 2020/2/15.
 */
public class Assembler {
    private static final String[] CONTROLS = {
            ".cursor",  // .cursor <from>
            ".span",    // .span <length>
            ".direct",  // .direct <data> <data> ...
            ".start",   // .start <from>
            ".length",  // .length <length>
    };

    private static final Set<String> CODE = new HashSet<String>();
    private static final Map<String, Integer> TO_CODE = new HashMap<String, Integer>();

    /*
        ("#i", "%s #%X");
        ("(a)", "%s (%X)");
        ("(d),y", "%s (%X),y");
        ("(d,x)", "%s (%X,x)");
        ("*+d", "%s %X");
        ("a", "%s %X");
        ("a,x", "%s %X,x");
        ("a,y", "%s %X,y");
        ("d", "%s %X");
        ("d,x", "%s %X,x");
        ("d,y", "%s %X,y");
        ("", "%s");
     */

    static {
        int i = 0;
        for (String[] r : Rules.RULES) {
            CODE.add(r[0]);
            TO_CODE.put(r[0] + r[1], i);
            i++;
        }
        TO_CODE.put("NOP", 0xEA);
        TO_CODE.put("SBC#i", 0xE9);
    }

    private int lineCount = 0;
    private int memoryStart = 0x8000;
    private int memoryLength = 0x8000;

    public byte[] assemble(String inputPath, String outputPath) throws IOException, AssemblerException {
        BufferedReader reader = new BufferedReader(new FileReader(inputPath));
        Map<String, Integer> refList = new HashMap<String, Integer>();
        Map<Integer, RefRequest> branchList = new HashMap<Integer, RefRequest>();
        Map<Integer, RefRequest> shortList = new HashMap<Integer, RefRequest>();
        Map<Integer, RefRequest> byteList = new HashMap<Integer, RefRequest>();
        byte[] memory = new byte[0x10000];
        boolean[] memoryUsed = new boolean[0x10000];
        int cursor = 0;

        lineCount = 0;

        while (true) {
            lineCount++;
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();

            if (line.startsWith(";") || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            int index = line.indexOf(':');
            while (index >= 0) {
                String ref = line.substring(0, index);
                refList.put(ref, cursor);
                line = line.substring(index + 1).trim();
                index = line.indexOf(':');
            }

            if (line.length() == 0) {
                continue;
            }

            if (line.endsWith(":")) {
                refList.put(line.substring(0, line.length() - 1), cursor);
            } else if (line.startsWith(".cursor ")) {
                cursor = parseNumber(line.substring(8).trim());
            } else if (line.startsWith(".span ")) {
                cursor += parseNumber(line.substring(6).trim());
            } else if (line.startsWith(".start ")) {
                memoryStart = parseNumber(line.substring(7).trim());
            } else if (line.startsWith(".length ")) {
                memoryLength = parseNumber(line.substring(8).trim());
            } else if (line.startsWith(".direct ")) {
                cursor += setDirect(memory, memoryUsed, cursor, line.substring(8).trim(), refList, shortList);
                cursor = cursor & 0xFFFF;
                if (memoryUsed[cursor]) {
                    throw new AssemblerException("Code conflict");
                }
            } else {
                cursor += setCode(memory, memoryUsed, cursor, line, refList, branchList, shortList, byteList);
                cursor = cursor & 0xFFFF;
                if (memoryUsed[cursor]) {
                    throw new AssemblerException("Code conflict");
                }
            }
            cursor = cursor & 0xFFFF;
        }

        for (Map.Entry<Integer, RefRequest> entry : branchList.entrySet()) {
            RefRequest request = entry.getValue();
            lineCount = request.line;
            String ref = request.ref;
            cursor = entry.getKey();
            Integer n = refList.get(ref);
            if (n != null) {
                int value = n - cursor - 2 + request.offset;
                if (value < -128 || value > 127) {
                    throw new AssemblerException("Branch out of range");
                }
                memory[(cursor + 1) & 0xFFFF] = (byte) value;
            } else {
                throw new AssemblerException("Cannot find ref " + ref);
            }
        }

        for (Map.Entry<Integer, RefRequest> entry : shortList.entrySet()) {
            RefRequest request = entry.getValue();
            lineCount = request.line;
            String ref = request.ref;
            cursor = entry.getKey();
            Integer n = refList.get(ref);
            if (n != null) {
                int value = n + request.offset;
                memory[(cursor + 1) & 0xFFFF] = (byte) value;
                memory[(cursor + 2) & 0xFFFF] = (byte) (value >> 8);
            } else {
                throw new AssemblerException("Cannot find ref " + ref);
            }
        }

        for (Map.Entry<Integer, RefRequest> entry : byteList.entrySet()) {
            RefRequest request = entry.getValue();
            lineCount = request.line;
            String ref = request.ref;
            cursor = entry.getKey();
            Integer n = refList.get(ref);
            if (n != null) {
                int value = n + request.offset;
                if (value >= 256 && !request.onlyHalf) {
                    throw new AssemblerException("Cannot put ref " + ref + " to an 8-bit argument");
                }
                if (request.onlyHalf && request.highHalf) {
                    value >>= 8;
                }
                memory[(cursor + 1) & 0xFFFF] = (byte) value;
            } else {
                throw new AssemblerException("Cannot find ref " + ref);
            }
        }

        if (outputPath != null) {
            FileOutputStream out = new FileOutputStream(outputPath);
            out.write(memory, memoryStart, memoryLength);
            out.close();
        }

        return Arrays.copyOfRange(memory, memoryStart, memoryStart + memoryLength);
    }

    public int getLineCount() {
        return lineCount;
    }

    private int setCode(byte[] memory, boolean[] memoryUsed, int cursor, String line, Map<String, Integer> refList, Map<Integer, RefRequest> branchList, Map<Integer, RefRequest> shortList, Map<Integer, RefRequest> byteList) throws AssemblerException {
        String[] split = line.split(" ");
        String code = split[0].toUpperCase();
        String args = split.length > 1 ? split[1] : "";

        String argsCode;
        int length;
        int value = 0;
        boolean[] preferZeroPage = new boolean[1];

        if (!CODE.contains(code)) {
            throw new AssemblerException("No opcode for " + code + " ...");
        }

        if (args.equals("")) {
            argsCode = "";
            length = 1;
        } else if (args.startsWith("#")) {
            argsCode = "#i";
            length = 2;
            String ref = args.substring(1);
            value = getNumValue(cursor, refList, shortList, byteList, code, ref, preferZeroPage);
            if (value > 255) {
                throw new AssemblerException("#i can only be 8 bits");
            }
        } else if (args.startsWith("(") && args.endsWith("),y")) {
            argsCode = "(d),y";
            length = 2;
            String ref = args.substring(1, args.length() - 3);
            value = getNumValue(cursor, refList, shortList, byteList, code, ref, preferZeroPage);
            if (value > 255) {
                throw new AssemblerException("(d),y can only address zero page");
            }
        } else if (args.startsWith("(") && args.endsWith(",x)")) {
            argsCode = "(d,x)";
            length = 2;
            String ref = args.substring(1, args.length() - 3);
            value = getNumValue(cursor, refList, shortList, byteList, code, ref, preferZeroPage);
            if (value > 255) {
                throw new AssemblerException("(d,x) can only address zero page");
            }
        } else if (args.startsWith("(") && args.endsWith(")")) {
            argsCode = "(a)";
            length = 3;
            String ref = args.substring(1, args.length() - 1);
            value = getNumValue(cursor, refList, shortList, byteList, code, ref, preferZeroPage);
        } else if (code.startsWith("B") && !code.equals("BRK") && !code.equals("BIT")) {
            argsCode = "*+d";
            length = 2;
            value = getBranchValue(cursor, refList, branchList, value, args);
        } else if (args.endsWith(",x")) {
            String ref = args.substring(0, args.length() - 2);
            value = getNumValue(cursor, refList, shortList, byteList, code, ref, preferZeroPage);
            if (value < 256 && preferZeroPage[0] && TO_CODE.containsKey(code + "d,x")) {
                argsCode = "d,x";
                length = 2;
            } else {
                argsCode = "a,x";
                length = 3;
            }
        } else if (args.endsWith(",y")) {
            String ref = args.substring(0, args.length() - 2);
            value = getNumValue(cursor, refList, shortList, byteList, code, ref, preferZeroPage);
            if (value < 256 && preferZeroPage[0] && TO_CODE.containsKey(code + "d,y")) {
                argsCode = "d,y";
                length = 2;
            } else {
                argsCode = "a,y";
                length = 3;
            }
        } else {
            value = getNumValue(cursor, refList, shortList, byteList, code, args, preferZeroPage);
            if (value < 256 && preferZeroPage[0] && TO_CODE.containsKey(code + "d")) {
                argsCode = "d";
                length = 2;
            } else {
                argsCode = "a";
                length = 3;
            }
        }

        Integer opcode = TO_CODE.get(code + argsCode);
        if (opcode == null) {
            throw new AssemblerException("No opcode for " + code + " " + args);
        }

        if (length == 1) {
            memory[cursor] = opcode.byteValue();
        } else if (length == 2) {
            memory[cursor] = opcode.byteValue();
            memory[(cursor + 1) & 0xFFFF] = (byte) value;
        } else if (length == 3) {
            memory[cursor] = opcode.byteValue();
            memory[(cursor + 1) & 0xFFFF] = (byte) value;
            memory[(cursor + 2) & 0xFFFF] = (byte) (value >> 8);
        }

        for (int i=0; i<length; i++) {
            memoryUsed[(cursor + i) & 0xFFFF] = true;
        }

        return length;
    }

    private int getBranchValue(int cursor, Map<String, Integer> refList, Map<Integer, RefRequest> branchList, int value, String ref) throws AssemblerException {
        Integer n = tryParseInteger(ref);
        if (n != null) {
            value = n - cursor - 2;
            if (value < -128 || value > 127) {
                throw new AssemblerException("Branch out of range");
            }
        } else {
            n = refList.get(ref);
            if (n != null) {
                value = n - cursor - 2;
                if (value < -128 || value > 127) {
                    throw new AssemblerException("Branch out of range");
                }
            } else {
                branchList.put(cursor, new RefRequest(lineCount, 0, ref));
            }
        }
        return value;
    }

    private int getNumValue(int cursor, Map<String, Integer> refList, Map<Integer, RefRequest> shortList, Map<Integer, RefRequest> byteList, String code, String ref, boolean[] preferZeroPage) throws AssemblerException {
        int value;
        boolean onlyHalf = false;
        boolean highHalf = false;
        boolean zeroPage = true;

        if ((ref.startsWith("a[") || ref.startsWith("A[")) && ref.endsWith("]")) {
            zeroPage = false;
            ref = ref.substring(2, ref.length() - 1);
        } else if ((ref.startsWith("l[") || ref.startsWith("L[")) && ref.endsWith("]")) {
            onlyHalf = true;
            highHalf = false;
            ref = ref.substring(2, ref.length() - 1);
        } else if ((ref.startsWith("h[") || ref.startsWith("H[")) && ref.endsWith("]")) {
            onlyHalf = true;
            highHalf = true;
            ref = ref.substring(2, ref.length() - 1);
        }

        if (code.equals("JMP") || code.equals("JSR")) {
            zeroPage = false;
        }

        int index = ref.indexOf('+');
        int offset = 0;
        if (index >= 0) {
            offset = parseNumber(ref.substring(index + 1));
            ref = ref.substring(0, index);
        }

        Integer n = tryParseInteger(ref);

        if (n != null) {
            value = n + offset;
        } else {
            n = refList.get(ref);
            if (n != null) {
                value = n + offset;
                if (onlyHalf) {
                    if (highHalf) {
                        value = (value >> 8) & 0xFF;
                    } else {
                        value &= 0xFF;
                    }
                }
            } else {
                if (onlyHalf) {
                    byteList.put(cursor, new RefRequest(lineCount, offset, ref, highHalf));
                } else if (zeroPage) {
                    byteList.put(cursor, new RefRequest(lineCount, offset, ref));
                } else {
                    shortList.put(cursor, new RefRequest(lineCount, offset, ref));
                }
                value = 0;
            }
        }
        if (preferZeroPage != null) {
            preferZeroPage[0] = zeroPage;
        }
        return value;
    }

    private int setDirect(byte[] memory, boolean[] memoryUsed, int cursor, String numbers, Map<String, Integer> refList, Map<Integer, RefRequest> shortList) throws AssemblerException {
        String[] split = numbers.split(" ");
        int i = 0;
        for (String str : split) {
            Integer n = tryParseInteger(str);
            if (n != null) {
                memory[cursor] = n.byteValue();
            } else {
                n = refList.get(str);
                if (n != null) {
                    memory[cursor] = (byte) (n & 0xFF);
                    memory[cursor + 1] = (byte) ((n >> 8) & 0xFF);
                } else {
                    shortList.put(cursor - 1, new RefRequest(lineCount, 0, str));
                }
                memoryUsed[cursor] = true;
                cursor++;
                i++;
            }
            memoryUsed[cursor] = true;
            cursor++;
            i++;
        }
        return i;
    }

    private int parseNumber(String str) throws AssemblerException {
        try {
            return str.startsWith("0x") ? Integer.parseInt(str.substring(2), 16) : Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new AssemblerException("Not a number " + str, e);
        }
    }

    private Integer tryParseInteger(String str) throws AssemblerException {
        try {
            return parseNumber(str);
        } catch (AssemblerException e) {
            if (e.getCause() instanceof NumberFormatException) {
                return null;
            }
            throw e;
        }
    }

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        Assembler assembler = new Assembler();
        byte[] memory = null;
        int memoryStart = 0;
        int memoryLength = 0;
        try {
            memory = assembler.assemble("code.asm", "a.out");
            memoryStart = assembler.memoryStart;
            memoryLength = assembler.memoryLength;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AssemblerException e) {
            System.err.printf("Line %d: %s\n", assembler.getLineCount(), e.getMessage());
        }
        System.out.println("Time: " + (System.nanoTime() - startTime) / 1e9 + "s");

        try {
            Memory m = new DefaultMemory(memory, 0, memoryLength);
            MemoryInputStream in = new MemoryInputStream(m, 0);
            AssembleTexter texter = new AssembleTexter(in, memoryStart);
            FileWriter writer = new FileWriter("a.dump");
            try {
                while (true) {
                    writer.write(texter.getNextInstruction());
                    writer.write('\n');
                }
            } catch (Exception e) {

            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
