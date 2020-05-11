package me.charlesj.apu;

/**
 * 2020/2/3.
 */
public class Sequencer {
    private int[] sequence;
    private int sequenceCounter;

    public Sequencer() {}

    public Sequencer(int[] sequence) {
        this.sequence = sequence;
        this.sequenceCounter = 0;
    }

    public void setSequence(int[] sequence) {
        this.sequence = sequence;
        sequenceCounter = sequenceCounter % sequence.length;
    }

    public void step() {
        sequenceCounter = (sequenceCounter + 1) % sequence.length;
    }

    public int output() {
        return sequence[sequenceCounter];
    }

    public void reset() {
        sequenceCounter = 0;
    }
}
