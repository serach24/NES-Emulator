package me.charlesj.mapper;

import me.charlesj.apu.APU;
import me.charlesj.input.Input;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.DefaultMemory;
import me.charlesj.memory.Memory;
import me.charlesj.memory.MixedMemory;
import me.charlesj.memory.ReadonlyMemory;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.cpu.CPU;
import me.charlesj.memory.*;
import me.charlesj.ppu.PPU;

/**
 * Mapper = 22, 23
 * 2020/2/2.
 */
public class VRC2 extends Mapper implements Memory {

    public static final int A = 0;
    public static final int B = 1;

    private final int reversion;

    private NesLoader loader;
    private CompositeMemory mainMemory;
    private CompositeMemory chrMemory;
    private PPU ppu;

    private int[] chrRegisters = new int[8];

    public VRC2(int reversion) {
        this.reversion = reversion;
    }

    @Override
    public void mapMemoryImpl(CompositeMemory memory, NesLoader loader, CPU cpu, PPU ppu, APU APU, Input input) {
        this.ppu = ppu;
        this.loader = loader;
        this.mainMemory = memory;

        memory.setMemory(0x6000, new DefaultMemory(0x2000));

        memory.setMemory(0x8000, new MixedMemory(0x2000, new ReadonlyMemory(loader.getPRGPage(0), 0, 0x2000), 0, this, 0));
        memory.setMemory(0xA000, new MixedMemory(0x2000, new ReadonlyMemory(loader.getPRGPage(0), 0x2000, 0x2000), 0, this, 0x2000));
        memory.setMemory(0xC000, new MixedMemory(0x4000, new ReadonlyMemory(loader.getPRGPage(loader.getPRGPageCount() - 1)), 0, this, 0x4000));

        chrMemory = new CompositeMemory(0x2000);
        for (int i=0; i<8; i++) {
            if (loader.getCHRPageCount() > 0) {
                chrMemory.setMemory(i * 0x400, new ReadonlyMemory(loader.getCHRPage(0), i * 0x400, 0x400));
            } else {
                chrMemory.setMemory(i * 0x400, new ReadonlyMemory(0x400));
            }
        }
        ppu.setCHRMemory(chrMemory);
    }

    public int getSize() {
        return 0x8000;
    }

    public int getByte(int address) {
        throw new UnsupportedOperationException("Cannot getByte from mapper register");
    }

    public void setByte(int address, int value) {
        if (reversion == A) {
            address = (address & 0xFFFC) | ((address & 2) >> 1) | ((address & 1) << 1);
        }
        switch (address) {
            case 0: case 1: case 2: case 3:
                mainMemory.setMemory(0x8000,
                        new MixedMemory(0x2000,
                                new ReadonlyMemory(loader.getPRGPage((value >> 1) & 0xF), (value & 1) == 0 ? 0 : 0x2000, 0x2000), 0,
                                this, 0
                        ));
                break;
            case 0x2000:case 0x2001:case 0x2002:case 0x2003:
                mainMemory.setMemory(0xA000,
                        new MixedMemory(0x2000,
                                new ReadonlyMemory(loader.getPRGPage((value >> 1) & 0xF), (value & 1) == 0 ? 0 : 0x2000, 0x2000), 0,
                                this, 0x2000
                        ));
                break;
            case 0x1000:case 0x1001:case 0x1002:case 0x1003:
                if ((value & 1) == 0) {
                    ppu.setMirroringType(PPU.VERTICAL_MIRRORING);
                } else {
                    ppu.setMirroringType(PPU.HORIZONTAL_MIRRORING);
                }
                break;
            case 0x3000:case 0x3001:case 0x3002:case 0x3003:
            case 0x4000:case 0x4001:case 0x4002:case 0x4003:
            case 0x5000:case 0x5001:case 0x5002:case 0x5003:
            case 0x6000:case 0x6001:case 0x6002:case 0x6003:
                int chrId = ((address >> 1) & 1) | ((((address >> 12) & 0xF) - 3) << 1);
                boolean isLow = (address & 1) == 0;
                if (isLow) {
                    chrRegisters[chrId] = (chrRegisters[chrId] & 0xF0) | (value & 0xF);
                } else {
                    chrRegisters[chrId] = (chrRegisters[chrId] & 0xF) | ((value & 0xF) << 4);
                }
                int registerValue = chrRegisters[chrId];
                if (reversion == A) {
                    registerValue >>= 1;
                }
                chrMemory.setMemory(0x400 * chrId, new ReadonlyMemory(loader.getCHRPage(registerValue >> 3), (registerValue & 0x7) * 0x400, 0x400));
                break;
            default:
                System.out.println(address + ", " + value);
                break;
        }
    }
}
