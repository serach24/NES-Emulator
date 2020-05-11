package me.charlesj.test;

import me.charlesj.apu.APU;
import me.charlesj.apu.SimpleAPU;
import me.charlesj.cpu.CPURegister;
import me.charlesj.cpu.SimpleCPU;
import me.charlesj.cpu.CPU;
import me.charlesj.input.StandardControllers;
import me.charlesj.mapper.Mapper;
import me.charlesj.mapper.MapperFactory;
import me.charlesj.memory.CompositeMemory;
import me.charlesj.memory.Memory;
import me.charlesj.nesloader.FileNesLoader;
import me.charlesj.nesloader.NesLoader;
import me.charlesj.ppu.PPU;
import me.charlesj.ppu.PPURegister;
import me.charlesj.ppu.SimplePPU;
import me.charlesj.screen.DefaultScreen;
import me.charlesj.screen.Screen;
import me.charlesj.speaker.Speaker;
import me.charlesj.util.AssembleTexter;
import me.charlesj.util.MemoryInputStream;
import me.charlesj.util.NesBuilder;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * This file is only for debug.
 * 2020/1/24.
 */
public class Debugger {

    private NesLoader loader = null;

    private PPU ppu = new SimplePPU();
    private CPU cpu = new SimpleCPU();
    private APU apu = new SimpleAPU();

    private StandardControllers controllers = new StandardControllers();

    private Memory memory = new CompositeMemory(0x10000);

    private final byte[] lock = new byte[0];

    private Set<Integer> breakpoints = new HashSet<Integer>();
    private boolean fastMode = false;

    private int lastPC = 0;
    private int savedPC = 0;

    private volatile boolean poweredUp = false;

    /**
     * This is removed because it's not a real test
     */
    @Test
    public void test() {
        try {
            testImpl();
        } catch (Exception ex) {
            ex.printStackTrace();
            panel.repaint();
        }

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void testImpl() throws IOException {
        System.out.println("build nes ...");
        NesBuilder.main("-c small.png small_palette.png -p code.asm -v -m 0 -o out.nes".split(" "));
        System.out.println("build nes done");
        loader = new FileNesLoader(new File("out.nes"));
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

        Mapper mapper = MapperFactory.createMapperFromId(loader.getMapper());
        if (mapper == null) {
            throw new RuntimeException("Unimplemented mapper: " + loader.getMapper());
        }
        mapper.mapMemory(loader, cpu, ppu, apu, controllers);

        memory = cpu.getMemory();

        //((DefaultMemory) internalMemory).setListener(0x038F, new MemoryListener() {
        //    public void onSet(int address, int value) {
        //        fastMode = false;
        //    }
        //    public void onGet(int address) {
        //    }
        //});

        //PrintStream ps = new PrintStream(new FileOutputStream("mario.txt"));
        //AssembleTexter texter = new AssembleTexter(new MemoryInputStream(memory, 0x8000), 0x8000);
        //try {
        //    while (true) {
        //        ps.println(texter.getNextInstruction());
        //    }
        //} catch (EOFException ignored) {
        //} finally {
        //    texter.close();
        //    ps.close();
        //}

        Speaker speaker = new Speaker() {
            public void set(int level) {

            }

            public byte[] output() {
                return new byte[0];
            }

            public void reset() {

            }
        };

        cpu.setMemory(memory);
        cpu.powerUp();

        apu.powerUp();

        poweredUp = true;


        long time = System.nanoTime();
        int count = 2000000000;
        long oldCycle = 0;
        boolean oldInBlank = false;

        boolean lastSet = false;
        int old778 = 0;

        for (int i = 0; i < count; i++) {
            for (int k = 0; k < 100; k++, i++) {
                lastPC = savedPC;
                savedPC = cpu.getRegister().getPc();
                //if (lastSet && !ppu.getRegister().getGenerateNMI()) {
                //    fastMode = false;
                //}
                //lastSet = ppu.getRegister().getGenerateNMI();
                //if (!lastSet && (memory.getByte(savedPC + 1) | (memory.getByte(savedPC + 2) << 8)) == 0x2000) {
                //    fastMode = false;
                //}
                //if (memory.getByte(0x13) % 2 != 0 && savedPC != 0xF5EC) {
                //    fastMode = false;
                //}
                //if (!lastSet && (ppu.getRegister().getByte(0) & 1) != 0) {
                //    System.out.printf("2400 set: %04X\n", savedPC);
                //    fastMode = false;
                //}
                //lastSet = (ppu.getRegister().getByte(0) & 1) != 0;
                //if ((old778 & 1) != 0 && (memory.getByte(0x778) & 1) == 0) {
                //    System.out.printf("778 change: %04X\n", savedPC);
                //    fastMode = false;
                //}
                //old778 = memory.getByte(0x778);
                //AssembleTexter assembleTexter = new AssembleTexter(new MemoryInputStream(memory, savedPC), savedPC);
                //stream.println(assembleTexter.getNextInstruction());
                //stream.printf("%04X: %04X\n", savedPC, ppu.getRegister().getPPUAddress());
                //Logger.log(String.format("Flags: %08d", Integer.parseInt(Integer.toBinaryString(cpu.getRegister().getFlags()))));
                //Logger.log(assembleTexter.getNextInstruction());
                if (!fastMode || (breakpoints.contains(cpu.getRegister().getPc()))) {
                    synchronized (lock) {
                        fastMode = false;
                        tryPaint();
                        try {
                            long waitStartTime = System.nanoTime();
                            lock.wait();
                            time += (System.nanoTime() - waitStartTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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
                        updatePic();
                    }
                    oldInBlank = ppu.inVerticalBlank();
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

    private BufferedImage image = null;
    private Screen screen = new DefaultScreen();

    private Font consolas = new Font("Consolas", 0, 14);

    private JPanel panel = new JPanel() {
        Color border = new Color(0x80000000, true);
        {
            setPreferredSize(new Dimension(PPU.SCREEN_WIDTH * 4, PPU.SCREEN_HEIGHT * 2));
        }
        @Override
        public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (!poweredUp) {
                return;
            }

            g.scale(2, 2);
            if (image != null) {
                g.drawImage(image, 0, 0, null);
            }

            g.setColor(Color.GREEN);
            Memory sqrRam = ppu.getSprRam();
            int h = ppu.getRegister().is8x16() ? 16 : 8;
            for (int i=0; i<256; i+=4) {
                int y = sqrRam.getByte(i);
                int x = sqrRam.getByte(i + 3);
                g.drawRect(x, y, 8, h);
            }

            g.scale(0.5, 0.5);

            //g.setColor(border);
            //int xoff = 2 * (ppu.getRegister().getXScrollConsiderBaseNameTableAddress() & 0x7);
            //int yoff = 0; //2 * (ppu.getRegister().getYScrollConsiderBaseNameTableAddress() & 0x7);
            //for (int i=0; i<=30; i++) {
            //    g.drawLine(-xoff, i * 16 - yoff, PPU.SCREEN_WIDTH * 2 - xoff, i * 16 - yoff);
            //}
            //for (int i=0; i<=32; i++) {
            //    g.drawLine(i * 16 - xoff, -yoff, i * 16 - xoff, PPU.SCREEN_HEIGHT * 2 - yoff);
            //}

            CPURegister r = cpu.getRegister();

            g.setColor(Color.WHITE);

            g.setFont(consolas);

            g.setColor(Color.PINK);
            g.fillRect(PPU.SCREEN_WIDTH * 2, 0, PPU.SCREEN_WIDTH, 14);

            g.setColor(Color.BLACK);
            if (loader != null) {
                AssembleTexter texter = new AssembleTexter(new MemoryInputStream(memory, savedPC), savedPC);
                for (int i=0; i<50; i++) {
                    try {
                        if (breakpoints.contains(texter.getCurrentOffset())) {
                            g.setColor(Color.CYAN);
                            g.fillRect(PPU.SCREEN_WIDTH * 2, i * 12, PPU.SCREEN_WIDTH, 12);
                            g.setColor(Color.BLACK);
                        }
                        g.drawString(texter.getNextInstruction(), PPU.SCREEN_WIDTH * 2, 12 * i + 12);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            g.drawString(String.format("A: %02X", r.getA()), PPU.SCREEN_WIDTH * 3, 12);
            g.drawString(String.format("X: %02X", r.getX()), PPU.SCREEN_WIDTH * 3, 24);
            g.drawString(String.format("Y: %02X", r.getY()), PPU.SCREEN_WIDTH * 3, 36);
            g.drawString(String.format("SP: 1%02X", r.getSp()), PPU.SCREEN_WIDTH * 3, 48);
            g.drawString(String.format("PC: %04X", savedPC), PPU.SCREEN_WIDTH * 3, 60);
            g.drawString(String.format("Flags: %1s %1s %1s %1s %1s %1s",
                    r.isNegative() ? "N" : "-",
                    r.isOverflow() ? "V" : "-",
                    r.isDecimal() ? "D" : "-",
                    r.isDisableInterrupt() ? "I" : "-",
                    r.isZero() ? "Z" : "-",
                    r.isCarry() ? "C" : "-"), PPU.SCREEN_WIDTH * 3, 72);

            g.drawString(String.format("Last PC: %04X", lastPC), PPU.SCREEN_WIDTH * 3, 84);
            g.drawString(String.format("Stack: %02X %02X %02X %02X",
                    memory.getByte(0x100 + r.getSp() + 1),
                    memory.getByte(0x100 + r.getSp() + 2),
                    memory.getByte(0x100 + r.getSp() + 3),
                    memory.getByte(0x100 + r.getSp() + 4)), PPU.SCREEN_WIDTH * 3, 96);

            PPURegister pr = ppu.getRegister();

            g.translate(0, 36);
            g.drawString(String.format("PPU Control1: %08d", Integer.parseInt(Integer.toString(pr.getByte(0), 2))), PPU.SCREEN_WIDTH * 3, 84);
            g.drawString(String.format("PPU Control2: %08d", Integer.parseInt(Integer.toString(pr.getByte(1), 2))), PPU.SCREEN_WIDTH * 3, 96);
            g.drawString(String.format("XScroll: %d", pr.getXScrollConsiderBaseNameTableAddress()), PPU.SCREEN_WIDTH * 3, 108);
            g.drawString(String.format("YScroll: %d", pr.getYScrollConsiderBaseNameTableAddress()), PPU.SCREEN_WIDTH * 3, 120);
            g.drawString(String.format("PPU Status: %08d", Integer.parseInt(Integer.toString(pr.getData(2), 2))), PPU.SCREEN_WIDTH * 3, 132);
            g.drawString(String.format("PPU Address: %04X", pr.getPPUAddress()), PPU.SCREEN_WIDTH * 3, 144);

            g.translate(PPU.SCREEN_WIDTH * 3, 156);
            g.drawString(String.format("Scanline: %d", ppu.getScanline()), 0, 0);
            g.drawString(String.format("Cycle: %d", ppu.getCycle()), 0, 12);

            g.translate(0, 36);
            g.drawString(String.format("Vectors: %04X %04X %04X",
                    memory.getByte(0xFFFA) | (memory.getByte(0xFFFB) << 8),
                    memory.getByte(0xFFFC) | (memory.getByte(0xFFFD) << 8),
                    memory.getByte(0xFFFE) | (memory.getByte(0xFFFF) << 8)
                    ), 0, 0);
            g.drawString(String.format("APU Interrupt: %s", String.valueOf(!apu.getRegister().isInterruptDisabled())), 0, 12);
        }
    };

    DefaultListModel<MemoryShower> model = new DefaultListModel<MemoryShower>();
    JPanel memoryShower = new JPanel() {
        {
            setPreferredSize(new Dimension(450, 14 * 0x400));
        }
        @Override
        public void paint(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setFont(consolas);
            g.setColor(Color.BLACK);

            for (int i=0; i<0x4000; i+=16) {
                g.drawString(toString(i), 0, 14 * (i / 16 + 1));
            }
        }

        public String toString(int start) {
            Memory memory = ((SimplePPU) ppu).getMemory();
            if (!poweredUp) {
                return "0";
            }
            return String.format("%04X: %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X",
                    start,
                    memory.getByte(start),
                    memory.getByte(start + 1),
                    memory.getByte(start + 2),
                    memory.getByte(start + 3),
                    memory.getByte(start + 4),
                    memory.getByte(start + 5),
                    memory.getByte(start + 6),
                    memory.getByte(start + 7),
                    memory.getByte(start + 8),
                    memory.getByte(start + 8 + 1),
                    memory.getByte(start + 8 + 2),
                    memory.getByte(start + 8 + 3),
                    memory.getByte(start + 8 + 4),
                    memory.getByte(start + 8 + 5),
                    memory.getByte(start + 8 + 6),
                    memory.getByte(start + 8 + 7)
            );
        }
    };
    JScrollPane outer = new JScrollPane(memoryShower);

    private JFrame frame = new JFrame();
    {
        frame.setLayout(new BorderLayout());

        for (int i=0; i<0x8000; i+=16) {
            model.addElement(new MemoryShower(i));
        }

        //outer.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        //memoryShower.setFocusable(false);
        //outer.setFocusable(false);
        //frame.add(outer, BorderLayout.EAST);

        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1:
                        synchronized (lock) {
                            lock.notify();
                        }
                        break;
                    case MouseEvent.BUTTON3: {
                        AssembleTexter texter = new AssembleTexter(new MemoryInputStream(memory, savedPC), savedPC);
                        int line = e.getY() / 12;
                        for (int i=0; i<line; i++) {
                            try {
                                texter.getNextInstruction();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        int currentOffset = texter.getCurrentOffset();
                        if (breakpoints.contains(currentOffset)) {
                            breakpoints.remove(currentOffset);
                        } else {
                            breakpoints.add(currentOffset);
                        }
                        panel.repaint();
                        break;
                    }
                    case MouseEvent.BUTTON2:
                        synchronized (lock) {
                            fastMode = true;
                            lock.notify();
                        }
                        break;
                }
            }
        });

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        String result = JOptionPane.showInputDialog("Add breakpoint:");
                        if (result == null) {
                            break;
                        }
                        String str = "+" + result.trim();
                        breakpoints.add(Integer.valueOf(str, 16));
                        panel.repaint();
                        break;
                    case KeyEvent.VK_ENTER:
                        synchronized (lock) {
                            fastMode = true;
                            lock.notify();
                        }
                        break;
                    case KeyEvent.VK_UP:
                        controllers.press(0, StandardControllers.KEY_UP);
                        break;
                    case KeyEvent.VK_DOWN:
                        controllers.press(0, StandardControllers.KEY_DOWN);
                        break;
                    case KeyEvent.VK_LEFT:
                        controllers.press(0, StandardControllers.KEY_LEFT);
                        break;
                    case KeyEvent.VK_RIGHT:
                        controllers.press(0, StandardControllers.KEY_RIGHT);
                        break;
                    case KeyEvent.VK_A:
                        controllers.press(0, StandardControllers.KEY_A);
                        break;
                    case KeyEvent.VK_S:
                        controllers.press(0, StandardControllers.KEY_B);
                        break;
                    case KeyEvent.VK_Q:
                        controllers.press(0, StandardControllers.KEY_SELECT);
                        break;
                    case KeyEvent.VK_W:
                        controllers.press(0, StandardControllers.KEY_START);
                        break;
                    default:
                        synchronized (lock) {
                            lock.notify();
                        }
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        controllers.release(0, StandardControllers.KEY_UP);
                        break;
                    case KeyEvent.VK_DOWN:
                        controllers.release(0, StandardControllers.KEY_DOWN);
                        break;
                    case KeyEvent.VK_LEFT:
                        controllers.release(0, StandardControllers.KEY_LEFT);
                        break;
                    case KeyEvent.VK_RIGHT:
                        controllers.release(0, StandardControllers.KEY_RIGHT);
                        break;
                    case KeyEvent.VK_A:
                        controllers.release(0, StandardControllers.KEY_A);
                        break;
                    case KeyEvent.VK_S:
                        controllers.release(0, StandardControllers.KEY_B);
                        break;
                    case KeyEvent.VK_Q:
                        controllers.release(0, StandardControllers.KEY_SELECT);
                        break;
                    case KeyEvent.VK_W:
                        controllers.release(0, StandardControllers.KEY_START);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void updatePic() {
        image = screen.show();
        tryPaint();
    }

    private void tryPaint() {
        savedPC = cpu.getRegister().getPc();
        panel.repaint();
        outer.repaint();
    }

    class MemoryShower {
        private int start;
        public MemoryShower(int start) {
            this.start = start;
        }
        @Override
        public String toString() {
            int j = start;
            if (!poweredUp) {
                return "0";
            }
            return String.format("%04X: %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X",
                    j,
                    memory.getByte(j),
                    memory.getByte(j + 1),
                    memory.getByte(j + 2),
                    memory.getByte(j + 3),
                    memory.getByte(j + 4),
                    memory.getByte(j + 5),
                    memory.getByte(j + 6),
                    memory.getByte(j + 7),
                    memory.getByte(j + 8),
                    memory.getByte(j + 8 + 1),
                    memory.getByte(j + 8 + 2),
                    memory.getByte(j + 8 + 3),
                    memory.getByte(j + 8 + 4),
                    memory.getByte(j + 8 + 5),
                    memory.getByte(j + 8 + 6),
                    memory.getByte(j + 8 + 7)
            );
        }
    }
}
