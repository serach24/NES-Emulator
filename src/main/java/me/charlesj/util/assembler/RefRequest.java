package me.charlesj.util.assembler;

/**
 * 2020/2/16.
 */
public class RefRequest {
    int line;
    String ref;
    int offset;
    boolean onlyHalf;
    boolean highHalf;
    public RefRequest(int line, int offset, String ref) {
        this.line = line;
        this.ref = ref;
        this.offset = offset;
        this.onlyHalf = false;
    }
    public RefRequest(int line, int offset, String ref, boolean highHalf) {
        this.line = line;
        this.ref = ref;
        this.offset = offset;
        this.highHalf = highHalf;
        this.onlyHalf = true;
    }
}
