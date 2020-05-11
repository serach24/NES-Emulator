package me.charlesj;

import me.charlesj.input.StandardControllers;
import me.charlesj.ppu.PPU;
import me.charlesj.screen.DefaultScreen;
import me.charlesj.screen.Screen;
import me.charlesj.speaker.DefaultSpeaker;
import me.charlesj.speaker.Speaker;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Emulator entry and UI.
 * 2020/1/22.
 */
public class Emulator extends JFrame implements Runnable, KeyListener {

    public static double CPU_CYCLE_PER_SECOND = 1789772.5;
    public static int SPEAKER_SAMPLE_RATE = 44100;

    private GameRunner gameRunner;

    private StandardControllers controllers = new StandardControllers();
    private Screen screen = new DefaultScreen();
    private Speaker speaker = new DefaultSpeaker(SPEAKER_SAMPLE_RATE);
    private EmulatorScreen emulatorScreen = new EmulatorScreen();
    private EmulatorSpeaker emulatorSpeaker = new EmulatorSpeaker(SPEAKER_SAMPLE_RATE);

    private Map<Integer, Integer> keyBindings = new HashMap<Integer, Integer>();

    public Emulator() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initFrame();
            }
        });
        keyBindings.put(KeyEvent.VK_UP, StandardControllers.KEY_UP);
        keyBindings.put(KeyEvent.VK_DOWN, StandardControllers.KEY_DOWN);
        keyBindings.put(KeyEvent.VK_LEFT, StandardControllers.KEY_LEFT);
        keyBindings.put(KeyEvent.VK_RIGHT, StandardControllers.KEY_RIGHT);
        keyBindings.put(KeyEvent.VK_A, StandardControllers.KEY_A);
        keyBindings.put(KeyEvent.VK_S, StandardControllers.KEY_B);
        keyBindings.put(KeyEvent.VK_Q, StandardControllers.KEY_SELECT);
        keyBindings.put(KeyEvent.VK_W, StandardControllers.KEY_START);
    }

    public void startGame(String game) {
        try {
            gameRunner = new GameRunner(game, controllers, screen, speaker, this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        emulatorSpeaker.setSpeaker(speaker);
        new Thread(emulatorSpeaker).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(gameRunner).start();
    }

    private void initFrame() {
        emulatorScreen.setScreen(screen);
        emulatorScreen.setPreferredSize(new Dimension(PPU.SCREEN_WIDTH * 2, PPU.SCREEN_HEIGHT * 2));
        emulatorScreen.setFocusable(false);

        addKeyListener(this);

        setLayout(new BorderLayout());
        add(emulatorScreen);
        pack();
        setTitle("NES Emulator");
        setIconImage(new ImageIcon(getClass().getResource("/nes.png")).getImage());
        setLocationRelativeTo(null);
    }

    public void run() {
        emulatorScreen.repaint();
    }

    public void keyPressed(KeyEvent e) {
        Integer r = keyBindings.get(e.getKeyCode());
        if (r != null) {
            controllers.press((r >> 8) & 1, r & 0xFF);
        }
    }

    public void keyReleased(KeyEvent e) {
        Integer r = keyBindings.get(e.getKeyCode());
        if (r != null) {
            controllers.release((r >> 8) & 1, r & 0xFF);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Emulator frame = new Emulator();

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("NES file", "nes"));
        fc.setMultiSelectionEnabled(false);
        fc.setCurrentDirectory(new File("."));
        fc.showOpenDialog(frame);

        if (fc.getSelectedFile() != null) {
            frame.startGame(fc.getSelectedFile().getAbsolutePath());
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
    }

    public void keyTyped(KeyEvent e) {}
}
