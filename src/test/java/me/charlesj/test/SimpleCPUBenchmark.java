package me.charlesj.test;

import me.charlesj.cpu.SimpleCPU;
import me.charlesj.cpu.CPU;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.DefaultMemory;
import me.charlesj.memory.Memory;
import me.charlesj.memory.MirrorMemory;
import me.charlesj.nesloader.InputStreamNesLoader;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.ppu.PPU;
import me.charlesj.ppu.SimplePPU;
import me.charlesj.screen.DefaultScreen;
import me.charlesj.screen.Screen;
import org.junit.Test;

import java.io.IOException;

/**
 * 2020/1/26.
 */
public class SimpleCPUBenchmark {
    @Test
    public void benchmark() throws IOException {
        NesLoader loader = new InputStreamNesLoader(getClass().getResourceAsStream("/game1.nes"));

        Screen screen = new DefaultScreen();

        PPU ppu = new SimplePPU();
        ppu.setCHRMemory(new DefaultMemory(loader.getCHRPage(0)));
        if (loader.isFourScreenMirroring()) {
            ppu.setMirroringType(PPU.FOUR_SCREEN_MIRRORING);
        } else if (loader.isHorizontalMirroring()) {
            ppu.setMirroringType(PPU.HORIZONTAL_MIRRORING);
        } else if (loader.isVerticalMirroring()) {
            ppu.setMirroringType(PPU.VERTICAL_MIRRORING);
        } else {
            ppu.setMirroringType(PPU.ONE_SCREEN_MIRRORING);
        }
        ppu.powerUp();

        CompositeMemory memory = new CompositeMemory(0x10000);
        Memory internalMemory = new DefaultMemory(0x8000);
        memory.setMemory(0, internalMemory);
        memory.setMemory(0x800, new MirrorMemory(internalMemory, 0x1800));
        memory.setMemory(0x2000, ppu.getRegister());
        memory.setMemory(0x2008, new MirrorMemory(ppu.getRegister(), 0x1FF8));
        memory.setMemory(0x4000, new DefaultMemory(0x4000));
        DefaultMemory rom = new DefaultMemory(loader.getPRGPage(0));
        memory.setMemory(0x8000, rom);
        memory.setMemory(0xC000, rom);

        CPU cpu = new SimpleCPU();
        cpu.setMemory(memory);
        cpu.powerUp();

        long time = System.nanoTime();
        int count = 2000000;
        long oldCycle = 0;
        for (int i = 0; i < count; i++) {
            for (int k = 0; k < 100; k++, i++) {
                int cycle = (int) (cpu.execute() - oldCycle);
                oldCycle = cpu.getCycle();
                for (int j = 0; j < cycle; j++) {
                    ppu.cycle(screen, cpu);
                    ppu.cycle(screen, cpu);
                    ppu.cycle(screen, cpu);
                }
            }
            double cps = cpu.getCycle() * 1e9 / (System.nanoTime() - time);
            while (cps > 1789772.5) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cps = cpu.getCycle() * 1e9 / (System.nanoTime() - time);
            }
        }

        double costTime = (System.nanoTime() - time) / 1e9;
        System.out.println("Cost time: " + costTime + "s");
        System.out.println("Cycles: " + cpu.getCycle());
        System.out.println("Instructions: " + count);
        System.out.println("CPI: " + cpu.getCycle() / (double) count);
        System.out.println("CPS: " + cpu.getCycle() / costTime / 1e6 + "M");
    }
}
