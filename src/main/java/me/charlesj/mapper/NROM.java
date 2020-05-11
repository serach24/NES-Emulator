package me.charlesj.mapper;

import me.charlesj.apu.APU;
import me.charlesj.input.Input;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.ReadonlyMemory;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.cpu.CPU;
import me.charlesj.ppu.PPU;

/**
 * Mapper = 0
 */
public class NROM extends Mapper {
    @Override
    public void mapMemoryImpl(CompositeMemory memory, NesLoader loader, CPU cpu, PPU ppu, APU APU, Input input) {
        memory.setMemory(0x8000, new ReadonlyMemory(loader.getPRGPage(0)));
        memory.setMemory(0xC000, new ReadonlyMemory(loader.getPRGPage(loader.getPRGPageCount() - 1)));
    }
}
