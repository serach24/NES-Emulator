package me.charlesj;

import me.charlesj.speaker.Speaker;

import javax.sound.sampled.*;

/**
 * 2020/2/3.
 */
public class EmulatorSpeaker implements Runnable {

    private volatile boolean stop = false;

    private final int sampleRate;
    private Speaker speaker;

    public EmulatorSpeaker(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setSpeaker(Speaker speaker) {
        this.speaker = speaker;
    }

    public void run() {
        try {
            runImpl();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void runImpl() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceLine.open(format);

        sourceLine.start();

        while (!stop) {
            byte[] b = speaker.output();
            sourceLine.write(b, 0, b.length);
        }

        sourceLine.drain();
        sourceLine.close();
    }

    public void stop() {
        stop = true;
    }
}
