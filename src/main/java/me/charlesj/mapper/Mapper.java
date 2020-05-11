package me.charlesj.mapper;

import me.charlesj.apu.APU;
import me.charlesj.apu.APURegister;
import me.charlesj.input.Input;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.DefaultMemory;
import me.charlesj.memory.Memory;
import me.charlesj.memory.MirrorMemory;
import me.charlesj.memory.MixedMemory;
import me.charlesj.memory.ReadonlyMemory;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.cpu.CPU;
import me.charlesj.cpu.IRQGenerator;
import me.charlesj.ppu.PPU;
import me.charlesj.ppu.SpriteDMARegister;

/**
 * Mapper class. Maps memory of CPU and PPU, and interrupts CPU.
 */
public abstract class Mapper {
    protected CompositeMemory initFirst4020BytesMemory(CPU cpu, PPU ppu, APU apu, Input input) {
        CompositeMemory memory = new CompositeMemory(0x10000);
        SpriteDMARegister dmaRegister = new SpriteDMARegister(memory, ppu.getSprRam(), cpu);
        APURegister apuRegister = apu.getRegister();

        Memory internalMemory = new DefaultMemory(0x800);
        memory.setMemory(0, internalMemory);
        memory.setMemory(0x800, new MirrorMemory(internalMemory, 0x1800));
        memory.setMemory(0x2000, ppu.getRegister());
        memory.setMemory(0x2008, new MirrorMemory(ppu.getRegister(), 0x1FF8));
        memory.setMemory(0x4000, apuRegister);
        memory.setMemory(0x4014, dmaRegister);
        memory.setMemory(0x4015, new MirrorMemory(apuRegister, 0x15, 1));
        memory.setMemory(0x4016, input);
        memory.setMemory(0x4017, new MixedMemory(1, input, 1, apuRegister, 0x17));
        memory.setMemory(0x4018, new MirrorMemory(apuRegister, 0x18, 0x4020 - 0x4018));
        return memory;
    }

    public void mapMemory(NesLoader loader, CPU cpu, PPU ppu, APU apu, Input input) {
        CompositeMemory memory = initFirst4020BytesMemory(cpu, ppu, apu, input);
        if (loader.isSRAMEnabled()) {
            memory.setMemory(0x6000, new DefaultMemory(0x2000));
        } else if (loader.is512ByteTrainerPresent()) {
            memory.setMemory(0x7000, new ReadonlyMemory(loader.getTrainer()));
        }

        if (loader.getCHRPageCount() != 0) {
            ppu.setCHRMemory(new DefaultMemory(loader.getCHRPage(0)));
        } else {
            ppu.setCHRMemory(new DefaultMemory(0x2000));
        }

        mapMemoryImpl(memory, loader, cpu, ppu, apu, input);

        cpu.setMemory(memory);

        cpu.addIRQGenerator(apu);
        if (this instanceof IRQGenerator) {
            cpu.addIRQGenerator((IRQGenerator) this);
        }
    }

    public abstract void mapMemoryImpl(CompositeMemory memory, NesLoader loader, CPU cpu, PPU ppu, APU APU, Input input);

    public void cycle(CPU cpu) {}
}
