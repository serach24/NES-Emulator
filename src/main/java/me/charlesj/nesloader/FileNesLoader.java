package me.charlesj.nesloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Loads NES file from a file.
 * 2020/1/22.
 */
public class FileNesLoader extends InputStreamNesLoader {
    public FileNesLoader(String filename) throws IOException {
        this(new File(filename));
    }

    public FileNesLoader(File file) throws IOException {
        super(new FileInputStream(file));
    }
}
