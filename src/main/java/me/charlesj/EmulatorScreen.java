package me.charlesj;

import me.charlesj.screen.Screen;

import javax.swing.*;
import java.awt.*;

/**
 * Simple emulator screen component.
 */
public class EmulatorScreen extends JComponent {
    private Screen screen;

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (screen != null) {
            g.drawImage(screen.show(), 0, 0, getWidth(), getHeight(), null);
        }
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }
}
