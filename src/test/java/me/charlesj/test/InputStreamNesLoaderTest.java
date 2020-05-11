package me.charlesj.test;

import me.charlesj.nesloader.InputStreamNesLoader;
import me.charlesj.nesloader.NesLoader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * 2020/1/22.
 */
public class InputStreamNesLoaderTest {
    @Test
    public void test() throws IOException {
        NesLoader loader = new InputStreamNesLoader(getClass().getResourceAsStream("/game1.nes"));
        Assert.assertEquals(1, loader.getPRGPageCount());
        Assert.assertEquals(1, loader.getCHRPageCount());
        Assert.assertEquals(0, loader.getMapper());
        Assert.assertEquals(true, loader.isVerticalMirroring());
        Assert.assertEquals(false, loader.is512ByteTrainerPresent());
        Assert.assertEquals(false, loader.isFourScreenMirroring());
        Assert.assertEquals(false, loader.isSRAMEnabled());
        Assert.assertEquals(16 * 1024, loader.getPRGPage(0).length);
    }
}
