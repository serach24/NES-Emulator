package me.charlesj.util;

import me.charlesj.util.assembler.AssemblerException;
import me.charlesj.util.patternconvert.PaletteLoader;
import me.charlesj.util.patternconvert.PatternBuilder;
import me.charlesj.util.assembler.Assembler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 2020/2/16.
 */
public class NesBuilder {
    private byte[][] prgROMPages = new byte[0][];
    private byte[][] chrROMPages = new byte[0][];
    private int prgPageCount;
    private int chrPageCount;
    private boolean isVerticalMirroring;
    private boolean isSRAMEnabled;
    private boolean is512ByteTrainerPresent;
    private boolean isFourScreenMirroring;
    private int mapper;
    private byte[] trainer;

    public NesBuilder setPrgRomPage(int index, byte[] page) {
        prgROMPages[index] = page;
        return this;
    }

    public NesBuilder setChrRomPage(int index, byte[] page) {
        chrROMPages[index] = page;
        return this;
    }

    public NesBuilder setPrgPageCount(int count) {
        prgPageCount = count;
        prgROMPages = Arrays.copyOf(prgROMPages, count);
        return this;
    }

    public NesBuilder setChrPageCount(int count) {
        chrPageCount = count;
        chrROMPages = Arrays.copyOf(chrROMPages, count);
        return this;
    }

    public NesBuilder setHorizontalMirroring() {
        isVerticalMirroring = false;
        return this;
    }

    public NesBuilder setVerticalMirroring() {
        isVerticalMirroring = true;
        return this;
    }

    public NesBuilder setSRAMEnabled(boolean SRAMEnabled) {
        isSRAMEnabled = SRAMEnabled;
        return this;
    }

    public NesBuilder setTrainer(byte[] trainer) {
        this.trainer = trainer;
        this.is512ByteTrainerPresent = trainer != null;
        return this;
    }

    public NesBuilder setFourScreenMirroring(boolean fourScreenMirroring) {
        isFourScreenMirroring = fourScreenMirroring;
        return this;
    }

    public NesBuilder setMapper(int mapper) {
        this.mapper = mapper;
        return this;
    }

    public void build(String outputFile) throws IOException {
        FileOutputStream out = new FileOutputStream(outputFile);
        try {
            build(out);
        } finally {
            out.close();
        }
    }

    private void build(FileOutputStream out) throws IOException {
        byte[] header = new byte[16];
        header[0] = 'N';
        header[1] = 'E';
        header[2] = 'S';
        header[3] = 0x1A;
        header[4] = (byte) prgPageCount;
        header[5] = (byte) chrPageCount;
        header[6] = (byte) (
                (isVerticalMirroring ? 1 : 0) |
                (isSRAMEnabled ? 2 : 0) |
                (is512ByteTrainerPresent ? 4 : 0) |
                (isFourScreenMirroring ? 8 : 0) |
                ((mapper & 0xF) << 4)
        );
        header[7] = (byte) (mapper & 0xF0);
        out.write(header);
        if (is512ByteTrainerPresent) {
            out.write(trainer, 0, 512);
        }
        for (int i=0; i<prgPageCount; i++) {
            out.write(prgROMPages[i], 0, 0x4000);
        }
        for (int i=0; i<chrPageCount; i++) {
            out.write(chrROMPages[i], 0, 0x2000);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
        }
        NesBuilder builder = new NesBuilder();
        String outputFileName = "a.nes";
        int pos = 0;
        while (pos < args.length) {
            String arg = args[pos];
            if (!arg.startsWith("-")) {
                usage();
            }
            switch (arg.charAt(1)) {
                case 'v':
                    builder.setVerticalMirroring();
                    break;
                case 'h':
                    builder.setHorizontalMirroring();
                    break;
                case '4':
                    builder.setFourScreenMirroring(true);
                    break;
                case 's':
                    builder.setSRAMEnabled(true);
                    break;
                case 'm':
                    if (pos == args.length - 1) {
                        usage();
                    }
                    pos++;
                    builder.setMapper(Integer.parseInt(args[pos]));
                    break;
                case 'o':
                    if (pos == args.length - 1) {
                        usage();
                    }
                    pos++;
                    outputFileName = args[pos];
                    break;
                case 't':
                    if (pos == args.length - 1) {
                        usage();
                    }
                    pos++;
                    String trainerFile = args[pos];
                    byte[] trainer = assemble(trainerFile);
                    builder.setTrainer(trainer);
                    break;
                case 'p': {
                    int i = pos + 1, n = 0;
                    while (i < args.length && !args[i].startsWith("-")) {
                        i++;
                        n++;
                    }
                    builder.setPrgPageCount(n);
                    for (i = 0; i < n; i++) {
                        pos++;
                        String prgFile = args[pos];
                        byte[] prgPage = assemble(prgFile);
                        builder.setPrgRomPage(i, prgPage);
                    }
                    break;
                }
                case 'c': {
                    int i = pos + 1, n = 0;
                    while (i < args.length && !args[i].startsWith("-")) {
                        i++;
                        n++;
                    }
                    builder.setChrPageCount((n / 2 + 1) / 2);
                    String chrPatternFile;
                    String chrPaletteFile;
                    int[][] palette;
                    for (i = 0; i < n; i+=4) {
                        pos++;
                        chrPatternFile = args[pos];
                        pos++;
                        chrPaletteFile = args[pos];
                        palette = PaletteLoader.loadPaletteFromFile(new File(chrPaletteFile));
                        byte[] data = PatternBuilder.build(chrPatternFile, palette, null);
                        data = Arrays.copyOf(data, 0x2000);
                        if (i + 2 < n - 1) {
                            pos++;
                            chrPatternFile = args[pos];
                            pos++;
                            chrPaletteFile = args[pos];
                            palette = PaletteLoader.loadPaletteFromFile(new File(chrPaletteFile));
                            byte[] data2 = PatternBuilder.build(chrPatternFile, palette, null);
                            System.arraycopy(data2, 0, data, 0x1000, 0x1000);
                        }
                        builder.setChrRomPage(i / 4, data);
                    }
                    break;
                }
            }
            pos++;
        }

        builder.build(outputFileName);
    }

    private static byte[] assemble(String filepath) throws IOException {
        Assembler assembler = new Assembler();
        byte[] result;
        try {
            result = assembler.assemble(filepath, null);
        } catch (AssemblerException e) {
            System.err.printf("Line %d: %s\n", assembler.getLineCount(), e.getMessage());
            result = null;
        }
        return result;
    }

    private static void usage() {
        System.out.println("Usage:   java NesBuilder <options>");
        System.out.println("Options: ");
        System.out.println("   -p <prg code1> [<prg code2> ...]");
        System.out.println("   -c <chr pattern 1> <chr palette 1> [<chr pattern 2> <chr palette 2> ...]");
        System.out.println("   -t <trainer code>");
        System.out.println("   -v(ertical mirroring)");
        System.out.println("   -h(orizontal mirroring)");
        System.out.println("   -4(screen mirroring)");
        System.out.println("   -s(ram enabled)");
        System.out.println("   -m <mapper id>");
        System.out.println("   -o <output filename>");

        System.exit(0);
    }
}
