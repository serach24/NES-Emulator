package me.charlesj.speaker;

import me.charlesj.Emulator;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Stores sound levels and output them.
 * 2020/2/3.
 */
public class DefaultSpeaker implements Speaker {

    private final double cyclePerSample;
    private final Queue<byte[]> queue = new LinkedList<byte[]>();
    private final int bufferSize;

    private byte[] buffer;
    private long cycle = 0;
    private int bufferId = 0;

    public DefaultSpeaker(int sampleRate) {
        this.cyclePerSample = Emulator.CPU_CYCLE_PER_SECOND / sampleRate;
        this.bufferSize = sampleRate / 441;
        this.buffer = new byte[bufferSize];
    }

    public void set(int level) {
        byte l = (byte) (level - 128);
        int bufferPos = (int) (cycle / cyclePerSample);
        int newBufferId = bufferPos / bufferSize;
        if (newBufferId != bufferId) {
            enqueue(buffer);
            buffer = new byte[bufferSize];
            bufferId = newBufferId;
        }
        buffer[bufferPos % bufferSize] = l;
        if (queue.size() < 2) {
            cycle++;
        }
    }

    public byte[] output() {
        return dequeue();
    }

    public void reset() {
        synchronized (queue) {
            queue.clear();
        }
    }

    private void enqueue(byte[] data) {
        synchronized (queue) {
            queue.add(data);
            queue.notify();
        }
    }

    private byte[] dequeue() {
        synchronized (queue) {
            while (queue.isEmpty()) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return queue.poll();
        }
    }
}
