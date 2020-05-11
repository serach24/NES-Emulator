package me.charlesj.nesloader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Load NES file from an InputStream.
 * 2020/1/23.
 */
public class InputStreamNesLoader implements NesLoader {
    private byte[][] prgROMPages;
    private byte[][] chrROMPages;
    private int prgPageCount;
    private int chrPageCount;
    private boolean isHorizontalMirroring;
    private boolean isVerticalMirroring;
    private boolean isSRAMEnabled;
    private boolean is512ByteTrainerPresent;
    private boolean isFourScreenMirroring;
    private int mapper;
    private byte[] trainer;

    public InputStreamNesLoader(InputStream in) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);
        byte[] b = new byte[16];
        dataIn.readFully(b);

        if (b[0] != 'N' || b[1] != 'E' || b[2] != 'S'   || b[3] != 0x1A) {
            throw new IOException("Not a NES file");
        }

        prgPageCount = b[4] & 0xFF;
        chrPageCount = b[5] & 0xFF;
        byte romControl1 = b[6];
        byte romControl2 = b[7];

        isHorizontalMirroring = (romControl1 & 1) == 0;
        isVerticalMirroring = !isHorizontalMirroring;
        isSRAMEnabled = ((romControl1 & 2) >>> 1) == 1;
        is512ByteTrainerPresent = ((romControl1 & 4) >>> 2) == 1;
        isFourScreenMirroring = ((romControl1 & 8) >>> 3) == 1;

        mapper = (romControl2 & 0xF0) + ((romControl1 & 0xF0) >>> 4);

        if (is512ByteTrainerPresent) {
            trainer = new byte[512];
            dataIn.readFully(trainer);
        } else {
            trainer = new byte[0];
        }

        prgROMPages = new byte[prgPageCount][16 * 1024];
        chrROMPages = new byte[chrPageCount][8 * 1024];

        for (int i=0; i<prgPageCount; i++) {
            dataIn.readFully(prgROMPages[i]);
        }
        for (int i=0; i<chrPageCount; i++) {
            dataIn.readFully(chrROMPages[i]);
        }

        dataIn.close();
    }

    public int getPRGPageCount() {
        return prgPageCount;
    }

    public int getCHRPageCount() {
        return chrPageCount;
    }

    public byte[] getPRGPage(int index) {
        return prgROMPages[index % prgPageCount];
    }

    public byte[] getCHRPage(int index) {
        return chrROMPages[index % chrPageCount];
    }

    public int getMapper() {
        return mapper;
    }

    public boolean isHorizontalMirroring() {
        return isHorizontalMirroring;
    }

    public boolean isVerticalMirroring() {
        return isVerticalMirroring;
    }

    public boolean isSRAMEnabled() {
        return isSRAMEnabled;
    }

    public boolean is512ByteTrainerPresent() {
        return is512ByteTrainerPresent;
    }

    public boolean isFourScreenMirroring() {
        return isFourScreenMirroring;
    }

    public byte[] getTrainer() {
        return trainer;
    }
}
