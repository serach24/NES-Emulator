package me.charlesj.apu;

import me.charlesj.cpu.CPU;
import me.charlesj.speaker.Speaker;

/**
 * Simple APU implementation.
 * 2020/2/3.
 */
public class SimpleAPU implements APU {

    private SoundGenerator mute = new MuteGenerator();

    private Pulse pulse1 = new Pulse(1);
    private Pulse pulse2 = new Pulse(2);
    private Triangle triangle = new Triangle();
    private Noise noise = new Noise();
    private DMC dmc = new DMC();

    private SoundGenerator[] generators = {pulse1, pulse2, triangle, noise, dmc};
    private APURegister r = new APURegister(generators);

    public void cycle(Speaker speaker, CPU cpu) {
        int oldPeriod = r.frameCounterTimer / APURegister.FRAME_COUNTER_PERIOD;
        int newPeriod = (r.frameCounterTimer + 1) / APURegister.FRAME_COUNTER_PERIOD;
        if (oldPeriod != newPeriod) {
            int stepCount = r.getStepMode();
            if ((newPeriod >= 1 && newPeriod <= 3) || newPeriod == stepCount) {
                for (SoundGenerator generator : generators) {
                    generator.clockEnvelopAndLinearCounter();
                }
            }
            if (newPeriod == 2 || newPeriod == stepCount) {
                for (SoundGenerator generator : generators) {
                    generator.clockLengthCounterAndSweep();
                }
            }
            if (!r.isInterruptDisabled() && newPeriod == stepCount && stepCount == 4) {
                r.setStatusFrameCounterInterrupt();
            }
            if (newPeriod == stepCount) {
                r.frameCounterTimer = -2;
            }
        }

        for (SoundGenerator generator : generators) {
            generator.cycle(cpu);
        }

        double pulseOut = 95.88 / ((8128.0 / (pulse1.output() + pulse2.output())) + 100);
        double tndOut = 159.79 / (100 + 1 / (triangle.output() / 8227.0 + noise.output() / 12241.0 + dmc.output() / 22638.0));
        double output = pulseOut + tndOut;

        if (output < 0) output = 0;
        if (output > 1) output = 1;

        speaker.set((int) (output * 255));

        r.frameCounterTimer++;
    }

    public APURegister getRegister() {
        return r;
    }

    public void writeRegister(int index, int value) {

    }

    public int readRegister(int index) {
        return 0;
    }

    public void powerUp() {
        reset();
    }

    public void reset() {
        r.reset();
    }

    public boolean getIRQLevel() {
        return r.getStatusFrameCounterInterrupt() || dmc.getIRQLevel();
    }
}
