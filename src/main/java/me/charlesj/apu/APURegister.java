package me.charlesj.apu;

import me.charlesj.memory.DefaultMemory;
import me.charlesj.memory.Memory;

import java.util.Arrays;

/**
 * Register used by APU. They are mapped to CPU memory.
 * 2020/2/3.
 */
public class APURegister extends DefaultMemory implements Memory {

    static final int FRAME_COUNTER_PERIOD = 7458;

    int frameCounterTimer;

    private SoundGenerator[] soundGenerators;

    public APURegister(SoundGenerator ... soundGenerators) {
        super(0x20);
        this.soundGenerators = soundGenerators;
    }

    public int getByte(int address) {
        int result = super.getByte(address);
        switch (address) {
            case 0x15: {
                int active = 0;
                int i = 0;
                for (SoundGenerator generator : soundGenerators) {
                    if (generator.isActive()) {
                        active |= (1 << i);
                    }
                    if (generator instanceof DMC) {
                        result |= ((DMC) generator).getInterruptFlag() ? 0x80 : 0;
                    }
                    i++;
                }
                result |= active;
                clearStatusFrameCounterInterrupt();
                break;
            }
            default:
                break;
        }
        return result;
    }

    public void setByte(int address, int value) {
        super.setByte(address, value);
        switch (address) {
            case 0x15:
                for (SoundGenerator generator : soundGenerators) {
                    generator.setEnabled((value & 1) != 0);
                    value >>= 1;
                }
                break;
            case 0x17:
                frameCounterTimer = -3;
                break;
            default:
                if ((address >> 2) < soundGenerators.length) {
                    soundGenerators[address >> 2].setRegister(address & 3, value);
                }
                break;
        }
    }

    public boolean isInterruptDisabled() {
        return (data[0x17] & 0x40) != 0;
    }

    public int getStepMode() {
        return (data[0x17] & 0x80) != 0 ? 5 : 4;
    }

    public void setStatusFrameCounterInterrupt() {
        data[0x15] |= 0x40;
    }

    public void clearStatusFrameCounterInterrupt() {
        data[0x15] &= ~0x40;
    }

    public boolean getStatusFrameCounterInterrupt() {
        return (data[0x15] &= 0x40) != 0;
    }

    public void reset() {
        Arrays.fill(data, (byte) 0);
        frameCounterTimer = -1;
    }
}
