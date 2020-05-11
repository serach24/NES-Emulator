package me.charlesj.mapper;

import me.charlesj.apu.APU;
import me.charlesj.input.Input;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.Memory;
import me.charlesj.memory.MixedMemory;
import me.charlesj.memory.ReadonlyMemory;
import me.charlesj.memory.WriteProtectMemory;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.cpu.CPU;
import me.charlesj.cpu.IRQGenerator;
import me.charlesj.ppu.PPU;
import me.charlesj.ppu.PPURegister;

import java.util.Arrays;

/**
 * MMC3/MMC6 mapper. Id is 4.
 * 2020/2/10.
 */
public class MMC3 extends Mapper implements Memory, IRQGenerator {

    private NesLoader loader;
    private PPU ppu;

    private CompositeMemory mainMemory;
    private Memory prgMemoryBankR6;
    private Memory prgMemoryBankM2;

    private CompositeMemory chrMemory;
    private CompositeMemory chrMemoryBankGroup1 = new CompositeMemory(0x1000);
    private CompositeMemory chrMemoryBankGroup2 = new CompositeMemory(0x1000);

    private WriteProtectMemory prgRam = new WriteProtectMemory(0x2000);

    private boolean prgBankMode;
    private boolean chrBankMode;
    private int rSelect;
    private int[] r = new int[8];
    private boolean[] rDirty = new boolean[8];

    private int irqCounter = 0;
    private int irqLatch = 0;
    private boolean irqReload = false;
    private boolean irqEnabled = false;
    private boolean irqPending = false;

    @Override
    public void mapMemoryImpl(CompositeMemory memory, NesLoader loader, CPU cpu, PPU ppu, APU APU, Input input) {
        this.ppu = ppu;
        this.loader = loader;
        this.mainMemory = memory;
        this.chrMemory = new CompositeMemory(0x2000);

        ppu.setCHRMemory(chrMemory);

        memory.setMemory(0x6000, prgRam);

        // fixed memory
        ReadonlyMemory prgMemoryBankM1;
        prgMemoryBankM1 = new ReadonlyMemory(loader.getPRGPage(loader.getPRGPageCount() - 1), 0x2000, 0x2000);
        prgMemoryBankM2 = new ReadonlyMemory(loader.getPRGPage(loader.getPRGPageCount() - 1), 0, 0x2000);

        memory.setMemory(0xE000, new MixedMemory(0x2000, prgMemoryBankM1, 0, this, 0x6000));

        // switchable memory
        Arrays.fill(rDirty, true);
        switchMemory(memory, chrMemory);
    }

    public int getSize() {
        return 0x8000;
    }

    public int getByte(int address) {
        throw new UnsupportedOperationException("Cannot getByte from mapper register");
    }

    @Override
    public void cycle(CPU cpu) {
        PPURegister r = ppu.getRegister();
        //The counter will not work properly unless you use different pattern tables for background and sprite data.
        //But makes stuck in some games.
        //if (r.getSpritePatternTableAddress() == r.getBackgroundPatternTableAddress()) {
        //    return;
        //}
        int cycle = ppu.getCycle();
        int scanline = ppu.getScanline();
        int cyclePos = r.getBackgroundPatternTableAddress() == 0 ? 260 : 324;
        if ((cycle >= cyclePos && cycle <= cyclePos + 2) && (scanline == 261 || scanline < 240)) {
            if (irqCounter == 0 || irqReload) {
                irqCounter = irqLatch;
                irqReload = false;
            } else {
                irqCounter--;
            }
            if (irqCounter == 0 && irqEnabled) {
                irqPending = true;
            }
        }
    }

    public boolean getIRQLevel() {
        return irqPending;
    }

    public void setByte(int address, int value) {
        switch (((address >> 12) & 0x6) | (address & 1)) {
            case 0:
                prgBankMode = (value & 0x40) != 0;
                chrBankMode = (value & 0x80) != 0;
                rSelect = value & 7;
                switchMemory(mainMemory, chrMemory);
                break;
            case 1:
                rDirty[rSelect] = true;
                r[rSelect] = value;
                switchMemory(mainMemory, chrMemory);
                break;
            case 2:
                if ((value & 1) == 0) {
                    ppu.setMirroringType(PPU.VERTICAL_MIRRORING);
                } else {
                    ppu.setMirroringType(PPU.HORIZONTAL_MIRRORING);
                }
                break;
            case 3:
                // Ram protect
                // They are ignored to avoid incompatibility with MMC6.
                break;
            case 4:
                irqLatch = value & 0xFF;
                break;
            case 5:
                irqReload = true;
                break;
            case 6:
                irqEnabled = false;
                irqPending = false;
                break;
            case 7:
                irqEnabled = true;
                break;
        }
    }

    private void switchMemory(CompositeMemory mainMemory, CompositeMemory chrMemory) {
        for (int i=0; i<2; i++) {
            if (rDirty[i]) {
                int v = r[i];
                chrMemoryBankGroup1.setMemory(0x800 * i, new ReadonlyMemory(loader.getCHRPage(v >> 3), ((v >> 1) & 0x3) * 0x800, 0x800));
            }
        }

        for (int i=2; i<6; i++) {
            if (rDirty[i]) {
                int v = r[i];
                chrMemoryBankGroup2.setMemory(0x400 * (i - 2), new ReadonlyMemory(loader.getCHRPage(v >> 3), (v & 0x7) * 0x400, 0x400));
            }
        }

        if (rDirty[6]) {
            int v = r[6] & 0x3F;
            prgMemoryBankR6 = new ReadonlyMemory(loader.getPRGPage(v >> 1), (v & 1) * 0x2000, 0x2000);
        }

        if (rDirty[7]) {
            int v = r[7] & 0x3F;
            Memory prgMemoryBankR7 = new ReadonlyMemory(loader.getPRGPage(v >> 1), (v & 1) * 0x2000, 0x2000);
            mainMemory.setMemory(0xA000, new MixedMemory(0x2000, prgMemoryBankR7, 0, this, 0x2000));
        }

        if (prgBankMode) {
            mainMemory.setMemory(0x8000, new MixedMemory(0x2000, prgMemoryBankM2, 0, this, 0));
            mainMemory.setMemory(0xC000, new MixedMemory(0x2000, prgMemoryBankR6, 0, this, 0x4000));
        } else {
            mainMemory.setMemory(0x8000, new MixedMemory(0x2000, prgMemoryBankR6, 0, this, 0));
            mainMemory.setMemory(0xC000, new MixedMemory(0x2000, prgMemoryBankM2, 0, this, 0x4000));
        }

        if (chrBankMode) {
            chrMemory.setMemory(0, chrMemoryBankGroup2);
            chrMemory.setMemory(0x1000, chrMemoryBankGroup1);
        } else {
            chrMemory.setMemory(0, chrMemoryBankGroup1);
            chrMemory.setMemory(0x1000, chrMemoryBankGroup2);
        }
    }
}
