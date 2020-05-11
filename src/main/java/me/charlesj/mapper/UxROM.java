package me.charlesj.mapper;

import me.charlesj.apu.APU;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.Memory;
import me.charlesj.memory.MixedMemory;
import me.charlesj.memory.ReadonlyMemory;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.cpu.CPU;
import me.charlesj.input.Input;
import me.charlesj.ppu.PPU;

/**
 * Mapper = 1
 */
public class UxROM extends Mapper implements Memory {

    private NesLoader loader;
    private CompositeMemory mainMemory;

    @Override
    public void mapMemoryImpl(CompositeMemory memory, NesLoader loader, CPU cpu, PPU ppu, APU APU, Input input) {
        this.loader = loader;
        this.mainMemory = memory;
        memory.setMemory(0x8000, new MixedMemory(0x4000, new ReadonlyMemory(loader.getPRGPage(0)), 0, this, 0));
        memory.setMemory(0xC000, new MixedMemory(0x4000, new ReadonlyMemory(loader.getPRGPage(loader.getPRGPageCount() - 1)), 0, this, 0x4000));
    }

    public int getSize() {
        return 0x8000;
    }

    public int getByte(int address) {
        throw new UnsupportedOperationException("Cannot getByte from mapper register");
    }

    public void setByte(int address, int value) {
        mainMemory.setMemory(0x8000, new MixedMemory(0x8000, new ReadonlyMemory(loader.getPRGPage(value & 0xF)), 0, this, 0));
    }
}
