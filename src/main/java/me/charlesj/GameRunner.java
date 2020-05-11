package me.charlesj;

import me.charlesj.apu.APU;
import me.charlesj.apu.SimpleAPU;
import me.charlesj.cpu.SimpleCPU;
import me.charlesj.cpu.CPU;
import me.charlesj.input.Input;
import me.charlesj.mapper.Mapper;
import me.charlesj.mapper.MapperFactory;
import me.charlesj.nesloader.FileNesLoader;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.ppu.PPU;
import me.charlesj.ppu.SimplePPU;
import me.charlesj.screen.Screen;
import me.charlesj.speaker.Speaker;

import java.io.IOException;

/**
 * Game thread.
 */
public class GameRunner implements Runnable {

    private volatile boolean stop = false;
    private volatile boolean pause = false;
    private final byte[] pauseLock = new byte[0];

    private final NesLoader loader;
    private final CPU cpu = new SimpleCPU();
    private final PPU ppu = new SimplePPU();
    private final APU apu = new SimpleAPU();
    private final Screen screen;
    private final Speaker speaker;
    private final Input input;
    private final Runnable repaintListener;

    private double fps = 60;
    private double cps = 1.7e6;

    public GameRunner(String filepath, Input input, Screen screen, Speaker speaker, Runnable repaintListener) throws IOException {
        this.loader = new FileNesLoader(filepath);
        this.screen = screen;
        this.speaker = speaker;
        this.input = input;
        this.repaintListener = repaintListener;
    }

    public void run() {
        if (loader.isFourScreenMirroring()) {
            ppu.setMirroringType(PPU.FOUR_SCREEN_MIRRORING);
        } else if (loader.isHorizontalMirroring()) {
            ppu.setMirroringType(PPU.HORIZONTAL_MIRRORING);
        } else if (loader.isVerticalMirroring()) {
            ppu.setMirroringType(PPU.VERTICAL_MIRRORING);
        } else {
            ppu.setMirroringType(PPU.ONE_SCREEN_MIRRORING);
        }

        Mapper mapper = MapperFactory.createMapperFromId(loader.getMapper());
        if (mapper == null) {
            throw new RuntimeException("Unimplemented mapper: " + loader.getMapper());
        }
        mapper.mapMemory(loader, cpu, ppu, apu, input);

        ppu.powerUp();
        cpu.powerUp();

        long time = System.nanoTime();
        long oldCycle = 0;
        long frame = 0;
        boolean oldInBlank = false;

        while (!stop) {
            for (int k = 0; k < 100; k++) {
                if (pause) {
                    synchronized (pauseLock) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                int cycle = (int) (cpu.execute() - oldCycle);
                oldCycle = cpu.getCycle();
                for (int j = 0; j < cycle; j++) {
                    mapper.cycle(cpu);
                    apu.cycle(speaker, cpu);
                    ppu.cycle(screen, cpu);
                    ppu.cycle(screen, cpu);
                    ppu.cycle(screen, cpu);
                    if (!oldInBlank && ppu.inVerticalBlank()) {
                        repaintListener.run();
                        frame++;
                    }
                    oldInBlank = ppu.inVerticalBlank();
                }
            }
            long timeDiff = System.nanoTime() - time;
            fps = frame * 1e9 / timeDiff;
            cps = cpu.getCycle() * 1e9 / timeDiff;
            while (cps > Emulator.CPU_CYCLE_PER_SECOND) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cps = cpu.getCycle() * 1e9 / (System.nanoTime() - time);
            }
        }
    }

    public void stop() {
        stop = true;
    }

    public void pause() {
        synchronized (pauseLock) {
            pause = true;
        }
    }

    public void resume() {
        synchronized (pauseLock) {
            pause = false;
            pauseLock.notifyAll();
        }
    }

    public double getCps() {
        return cps;
    }

    public double getFps() {
        return fps;
    }
}
