package me.charlesj.cpu;

/**
 * Registers used by cpu.
 * 2020/1/22.
 */
public class CPURegister {
    static final int MASK_NEGATIVE = 0x80;
    static final int MASK_OVERFLOW = 0x40;
    static final int MASK_BREAK = 0x10;
    static final int MASK_DECIMAL = 0x8;
    static final int MASK_INTERRUPT = 0x4;
    static final int MASK_ZERO = 0x2;
    static final int MASK_CARRY = 0x1;

    private int a;
    private int x;
    private int y;
    private int sp;
    private int pc;
    private int flags;

    void reset() {
        a = 0;
        x = 0;
        y = 0;
        sp = 0xfd;
        flags = 0x34;
    }

    public int getCarry() {
        return isCarry() ? 1 : 0;
    }

    public boolean isNegative() {
        return (flags & MASK_NEGATIVE) != 0;
    }

    public boolean isOverflow() {
        return (flags & MASK_OVERFLOW) != 0;
    }

    public boolean isBreak() {
        return (flags & MASK_BREAK) != 0;
    }

    public boolean isDecimal() {
        return (flags & MASK_DECIMAL) != 0;
    }

    public boolean isDisableInterrupt() {
        return (flags & MASK_INTERRUPT) != 0;
    }

    public boolean isZero() {
        return (flags & MASK_ZERO) != 0;
    }

    public boolean isCarry() {
        return (flags & MASK_CARRY) != 0;
    }

    public void setNegative() {
        flags |= MASK_NEGATIVE;
    }

    public void setOverflow() {
        flags |= MASK_OVERFLOW;
    }

    public void setBreak() {
        flags |= MASK_BREAK;
    }

    public void setDecimal() {
        flags |= MASK_DECIMAL;
    }

    public void setDisableInterrupt() {
        flags |= MASK_INTERRUPT;
    }

    public void setZero() {
        flags |= MASK_ZERO;
    }

    public void setCarry() {
        flags |= MASK_CARRY;
    }

    public void clearNegative() {
        flags &= ~MASK_NEGATIVE;
    }

    public void clearOverflow() {
        flags &= ~MASK_OVERFLOW;
    }

    public void clearBreak() {
        flags &= ~MASK_BREAK;
    }

    public void clearDecimal() {
        flags &= ~MASK_DECIMAL;
    }

    public void clearDisableInterrupt() {
        flags &= ~MASK_INTERRUPT;
    }

    public void clearZero() {
        flags &= ~MASK_ZERO;
    }

    public void clearCarry() {
        flags &= ~MASK_CARRY;
    }

    public void setNegative(boolean v) {
        if (v) setNegative(); else clearNegative();
    }

    public void setZero(boolean v) {
        if (v) setZero(); else clearZero();
    }

    public void setOverflow(boolean v) {
        if (v) setOverflow(); else clearOverflow();
    }

    public void setCarry(boolean v) {
        if (v) setCarry(); else clearCarry();
    }

    public int getA() {
        return a;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSp() {
        return sp;
    }

    public int getPc() {
        return pc;
    }

    public int getFlags() {
        return flags;
    }

    void setA(int a) {
        this.a = a & 0xFF;
    }

    void setX(int x) {
        this.x = x & 0xFF;
    }

    void setY(int y) {
        this.y = y & 0xFF;
    }

    void setSp(int sp) {
        this.sp = sp & 0xFF;
    }

    void setPc(int pc) {
        this.pc = pc & 0xFFFF;
    }

    void setFlags(int flags) {
        this.flags = flags & 0xFF;
    }
}
