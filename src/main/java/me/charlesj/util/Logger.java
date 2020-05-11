package me.charlesj.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Global logger for debug.
 * 2020/1/27.
 */
public class Logger {
    private static PrintStream stream;
    static {
        init();
    }

    private static void init() {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("log.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        stream = new PrintStream(out);
    }

    public static void log(String message) {
        stream.println(message);
    }

    public static void close() {
        stream.close();
    }
}
