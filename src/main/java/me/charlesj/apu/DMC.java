package me.charlesj.apu;

import me.charlesj.cpu.CPU;
import me.charlesj.cpu.IRQGenerator;

/**
 * Delta modulation channel.
 * 2020/2/5.
 */
public class DMC implements SoundGenerator, DividerListener, IRQGenerator {

    private static final int[] RATE_MAPPER = {
        428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
    };

    private Divider timer = new DecrementDivider();
    private boolean interruptFlag;
    private boolean irqEnabled;
    private boolean loopFlag;
    private int period;
    private int sampleAddress;
    private int sampleLength;

    private int currentSampleAddress;
    private int bytesRemaining;
    private boolean sampleBufferEmpty = true;
    private int sampleBuffer;

    private boolean silenceFlag;
    private int shiftRegister;
    private int bitsRemainingCounter;
    private int outputLevel;

    private CPU cpu;

    private boolean evenCycle = false;

    public DMC() {
        timer.setOutputClock(this);
    }

    public void cycle(CPU cpu) {
        evenCycle = !evenCycle;
        if (!evenCycle) {
            return;
        }

        this.cpu = cpu;
        timer.clock();
        this.cpu = null;
    }

    public void onClock(Divider divider) {
        if (bitsRemainingCounter == 0) {
            bitsRemainingCounter = 8;
            reloadSampleBuffer();
            if (sampleBufferEmpty) {
                silenceFlag = true;
            } else {
                silenceFlag = false;
                shiftRegister = sampleBuffer;
                sampleBufferEmpty = true;
            }
        }

        if (!silenceFlag) {
            int bit0 = shiftRegister & 1;
            if (bit0 == 1 && outputLevel < 126) {
                outputLevel += 2;
            } else if (bit0 == 0 && outputLevel >= 2) {
                outputLevel -= 2;
            }
        }
        shiftRegister >>= 1;
        bitsRemainingCounter--;
    }

    private void reloadSampleBuffer() {
        if (!sampleBufferEmpty || bytesRemaining == 0) {
            return;
        }

        sampleBuffer = cpu.getMemory().getByte(currentSampleAddress);
        sampleBufferEmpty = false;

        currentSampleAddress++;
        if (currentSampleAddress == 0x10000) {
            currentSampleAddress = 0x8000;
        }
        bytesRemaining--;
        if (bytesRemaining == 0) {
            if (loopFlag) {
                restart(false);
            } else if (irqEnabled) {
                interruptFlag = true;
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.interruptFlag = false;
        if (!enabled) {
            this.bytesRemaining = 0;
        }
        if (enabled && bytesRemaining == 0) {
            restart(true);
        }
    }

    private void restart(boolean bufferEmpty) {
        currentSampleAddress = sampleAddress;
        bytesRemaining = sampleLength;
        sampleBufferEmpty = bufferEmpty;
    }

    public void setRegister(int index, int value) {
        switch (index) {
            case 0:
                setIRQEnabled((value & 0x80) != 0);
                loopFlag = (value & 0x40) != 0;
                period = RATE_MAPPER[value & 0xF];
                timer.setPeriod(period);
                break;
            case 1:
                outputLevel = value & 0x7F;
                break;
            case 2:
                sampleAddress = 0xC000 | ((value & 0xFF) << 6);
                break;
            case 3:
                sampleLength = ((value & 0xFF) << 4) | 1;
                break;
            default:
                break;
        }
    }

    public int output() {
        return outputLevel;
    }

    public boolean isActive() {
        return bytesRemaining > 0;
    }

    public void setIRQEnabled(boolean enabled) {
        irqEnabled = enabled;
        if (!irqEnabled) {
            interruptFlag = false;
        }
    }

    public boolean getInterruptFlag() {
        return interruptFlag;
    }

    public void clockLengthCounterAndSweep() {}

    public void clockEnvelopAndLinearCounter() {}

    public boolean getIRQLevel() {
        return interruptFlag;
    }
}
