package me.charlesj.cpu;

import me.charlesj.memory.Memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple 6502 CPU implementation.
 * Unofficial opcodes are not supported.
 * 2020/1/23.
 */
public class SimpleCPU implements CPU {

    private static final Random RAND = new Random();

    private Memory m = null;
    private CPURegister r = new CPURegister();
    private long cycle = 0;

    private List<IRQGenerator> irqGenerators = new ArrayList<IRQGenerator>();
    private int pendingNMI = 0;

    public long execute() {
        if (checkIRQ()) {
            return cycle;
        }
        if (pendingNMI == 2) {
            pendingNMI--;
        } else if (pendingNMI == 1) {
            pendingNMI--;
            nmiImpl();
            return cycle;
        }

        int opcode = m.getByte(r.getPc());
        int opcodeRow = opcode & 0xE3;
        int addressingMethod = (opcode >> 2) & 7;

        r.setPc(r.getPc() + 1);

        switch (opcodeRow) {
            case 0x01: //ORA
                return ora(addressingMethod);
            case 0x21: //AND
                return and(addressingMethod);
            case 0x41: //XOR
                return xor(addressingMethod);
            case 0x61: //ADC
                return adc(addressingMethod);
            case 0x81: //STA
                if (addressingMethod != 2)
                    return sta(addressingMethod);
                break;
            case 0xA1: //LDA
                return lda(addressingMethod);
            case 0xC1: //CMP
                return cmp(addressingMethod);
            case 0xE1: //SBC
                return sbc(addressingMethod);
            case 0x02: //ASL
                if (addressingMethod == 2 || (addressingMethod & 1) == 1)
                    return asl(addressingMethod);
                break;
            case 0x22: //ROL
                if (addressingMethod == 2 || (addressingMethod & 1) == 1)
                    return rol(addressingMethod);
                break;
            case 0x42: //LSR
                if (addressingMethod == 2 || (addressingMethod & 1) == 1)
                    return lsr(addressingMethod);
                break;
            case 0x62: //ROR
                if (addressingMethod == 2 || (addressingMethod & 1) == 1)
                    return ror(addressingMethod);
                break;
            case 0x82: //STX
                if (addressingMethod != 7 && (addressingMethod & 1) == 1)
                    return stx(addressingMethod);
                break;
            case 0xA2: //LDX
                if (addressingMethod == 0 || (addressingMethod & 1) == 1)
                    return ldx(addressingMethod);
                break;
            case 0xC2: //DEC
                if ((addressingMethod & 1) == 1)
                    return dec(addressingMethod);
                break;
            case 0xE2: //INC
                if ((addressingMethod & 1) == 1)
                    return inc(addressingMethod);
                break;
            case 0x80: //STY
                if (addressingMethod != 7 && (addressingMethod & 1) == 1)
                    return sty(addressingMethod);
                break;
            case 0xA0: //LDY
                if (addressingMethod == 0 || (addressingMethod & 1) == 1)
                    return ldy(addressingMethod);
                break;
            case 0xC0: //CPY
                if (addressingMethod < 2 || addressingMethod == 3)
                    return cpy(addressingMethod);
                break;
            case 0xE0: //CPX
                if (addressingMethod < 2 || addressingMethod == 3)
                    return cpx(addressingMethod);
                break;
            default:
                break;
        }

        int col = opcode & 0x1F;
        if (col == 0x10) {
            return branch(opcode >> 5);
        }
        if (col == 0x18 && opcode != 0x98) {
            return status(opcode >> 5);
        }

        switch (opcode) {
            case 0x00:
                return brk();
            case 0x24:
            case 0x2C:
                return bit(addressingMethod);
            case 0x4C:
            case 0x6C:
                return jmp(opcode);
            case 0x20:
                return jsr();
            case 0xEA:
                return nop();
            case 0x40:
                return rti();
            case 0x60:
                return rts();
            case 0x88:
                return dey();
            case 0xCA:
                return dex();
            case 0xC8:
                return iny();
            case 0xE8:
                return inx();
            case 0x8A:
                return txa();
            case 0xAA:
                return tax();
            case 0x98:
                return tya();
            case 0xA8:
                return tay();
            case 0x9A:
                return txs();
            case 0xBA:
                return tsx();
            case 0x08:
                return php();
            case 0x28:
                return plp();
            case 0x48:
                return pha();
            case 0x68:
                return pla();
        }

        throw new RuntimeException("Unknown opcode " + Integer.toHexString(opcode));
    }

    private long nop() {
        cycle += 2;
        return cycle;
    }

    private long adc(int addressingMethod) {
        int value = getValue1(addressingMethod, false);
        int v = r.getA() + value + r.getCarry();
        r.setCarry(v > 255);
        r.setOverflow(((v & 0x80) == 0 && (r.getA() & 0x80) != 0 && (value & 0x80) != 0) || ((v & 0x80) != 0 && (r.getA() & 0x80) == 0 && (value & 0x80) == 0));
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v & 0xFF);
        return cycle;
    }

    private long sbc(int addressingMethod) {
        int value = getValue1(addressingMethod, false);
        int v = r.getA() - value - (1 - r.getCarry());
        r.setCarry(v >= 0);
        r.setOverflow(((v & 0x80) == 0 && (r.getA() & 0x80) != 0 && (value & 0x80) == 0) || ((v & 0x80) != 0 && (r.getA() & 0x80) == 0 && (value & 0x80) != 0));
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v & 0xFF);
        return cycle;
    }

    private long ora(int addressingMethod) {
        int value = getValue1(addressingMethod, false);
        int v = r.getA() | value;
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v & 0xFF);
        return cycle;
    }

    private long and(int addressingMethod) {
        int value = getValue1(addressingMethod, false);
        int v = r.getA() & value;
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v & 0xFF);
        return cycle;
    }

    private long xor(int addressingMethod) {
        int value = getValue1(addressingMethod, false);
        int v = r.getA() ^ value;
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v & 0xFF);
        return cycle;
    }

    private long bit(int addressingMethod) {
        int value = getValue3(addressingMethod, false);
        int v = r.getA() & value;
        setNegativeByValue(value);
        r.setOverflow((value & 0x40) != 0);
        setZeroByValue(v);
        return cycle;
    }

    private long cmp(int addressingMethod) {
        int value = getValue1(addressingMethod, false);
        int v = r.getA() - value;
        r.setCarry(v >= 0);
        setZeroByValue(v);
        setNegativeByValue(v);
        return cycle;
    }

    private long cpx(int addressingMethod) {
        int value = getValue3(addressingMethod, false);
        int v = r.getX() - value;
        r.setCarry(v >= 0);
        setZeroByValue(v);
        setNegativeByValue(v);
        return cycle;
    }

    private long cpy(int addressingMethod) {
        int value = getValue3(addressingMethod, false);
        int v = r.getY() - value;
        r.setCarry(v >= 0);
        setZeroByValue(v);
        setNegativeByValue(v);
        return cycle;
    }

    private long sta(int addressingMethod) {
        int address = getAddress1(addressingMethod, true);
        setMemoryValue(address, r.getA());
        return cycle;
    }

    private long lda(int addressingMethod) {
        int v = getValue1(addressingMethod, false);
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v);
        return cycle;
    }

    private long stx(int addressingMethod) {
        int address = getAddress2(addressingMethod, true);
        setMemoryValue(address, r.getX());
        return cycle;
    }

    private long ldx(int addressingMethod) {
        int v = getValue2(addressingMethod, false);
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setX(v);
        return cycle;
    }

    private long sty(int addressingMethod) {
        int address = getAddress3(addressingMethod, true);
        setMemoryValue(address, r.getY());
        return cycle;
    }

    private long ldy(int addressingMethod) {
        int v = getValue3(addressingMethod, false);
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setY(v);
        return cycle;
    }

    private long asl(int addressingMethod) {
        cycle += 2;
        if (addressingMethod == 2) {
            int v = r.getA() << 1;
            r.setCarry((v & 0x100) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            r.setA(v & 0xFF);
        } else {
            int address = getAddress3(addressingMethod, true);
            int v = m.getByte(address) << 1;
            r.setCarry((v & 0x100) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            setMemoryValue(address, v);
        }
        return cycle;
    }

    private long rol(int addressingMethod) {
        cycle += 2;
        if (addressingMethod == 2) {
            int v = (r.getA() << 1) | r.getCarry();
            r.setCarry((v & 0x100) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            r.setA(v & 0xFF);
        } else {
            int address = getAddress3(addressingMethod, true);
            int v = (m.getByte(address) << 1) | r.getCarry();
            r.setCarry((v & 0x100) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            setMemoryValue(address, v);
        }
        return cycle;
    }

    private long lsr(int addressingMethod) {
        cycle += 2;
        if (addressingMethod == 2) {
            int v = r.getA() >> 1;
            r.setCarry((r.getA() & 1) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            r.setA(v & 0xFF);
        } else {
            int address = getAddress3(addressingMethod, true);
            int value = m.getByte(address);
            int v = value >> 1;
            r.setCarry((value & 1) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            setMemoryValue(address, v);
        }
        return cycle;
    }

    private long ror(int addressingMethod) {
        cycle += 2;
        if (addressingMethod == 2) {
            int v = (r.getA() >> 1) | (r.getCarry() << 7);
            r.setCarry((r.getA() & 1) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            r.setA(v & 0xFF);
        } else {
            int address = getAddress3(addressingMethod, true);
            int value = m.getByte(address);
            int v = (value >> 1) | (r.getCarry() << 7);
            r.setCarry((value & 1) != 0);
            setZeroByValue(v);
            setNegativeByValue(v);
            setMemoryValue(address, v);
        }
        return cycle;
    }

    private long dec(int addressingMethod) {
        cycle += 2;
        int address = getAddress3(addressingMethod, true);
        int value = m.getByte(address);
        int v = value - 1;
        setZeroByValue(v);
        setNegativeByValue(v);
        setMemoryValue(address, v);
        return cycle;
    }

    private long inc(int addressingMethod) {
        cycle += 2;
        int address = getAddress3(addressingMethod, true);
        int value = m.getByte(address);
        int v = value + 1;
        setZeroByValue(v);
        setNegativeByValue(v);
        setMemoryValue(address, v);
        return cycle;
    }

    private long dex() {
        cycle += 2;
        int v = r.getX() - 1;
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setX(v & 0xFF);
        return cycle;
    }

    private long dey() {
        cycle += 2;
        int v = r.getY() - 1;
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setY(v & 0xFF);
        return cycle;
    }

    private long inx() {
        cycle += 2;
        int v = r.getX() + 1;
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setX(v & 0xFF);
        return cycle;
    }

    private long iny() {
        cycle += 2;
        int v = r.getY() + 1;
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setY(v & 0xFF);
        return cycle;
    }

    private long branch(int branchType) {
        cycle += 2;
        boolean condition = false;
        switch (branchType) {
            case 0: //BPL
                condition = !r.isNegative();
                break;
            case 1: //BMI
                condition = r.isNegative();
                break;
            case 2: //BVC
                condition = !r.isOverflow();
                break;
            case 3: //BVS
                condition = r.isOverflow();
                break;
            case 4: //BCC
                condition = !r.isCarry();
                break;
            case 5: //BCS
                condition = r.isCarry();
                break;
            case 6: //BNE
                condition = !r.isZero();
                break;
            case 7:
                condition = r.isZero();
                break;
            default:
        }
        int offset = (byte) m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        if (condition) {
            cycle += 1;
            int oldpc = r.getPc();
            r.setPc(r.getPc() + offset);
            if ((oldpc >> 8) != (r.getPc() >> 8)) {
                cycle += 1;
            }
        }
        return cycle;
    }

    private long txa() {
        cycle += 2;
        int v = r.getX();
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v);
        return cycle;
    }

    private long tax() {
        cycle += 2;
        int v = r.getA();
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setX(v);
        return cycle;
    }

    private long tya() {
        cycle += 2;
        int v = r.getY();
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setA(v);
        return cycle;
    }

    private long tay() {
        cycle += 2;
        int v = r.getA();
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setY(v);
        return cycle;
    }

    private long txs() {
        cycle += 2;
        int v = r.getX();
        //possibly not changed
        //setZeroByValue(v);
        //setNegativeByValue(v);
        r.setSp(v);
        return cycle;
    }

    private long tsx() {
        cycle += 2;
        int v = r.getSp();
        setZeroByValue(v);
        setNegativeByValue(v);
        r.setX(v);
        return cycle;
    }

    private long php() {
        cycle += 3;
        push(r.getFlags());
        return cycle;
    }

    private long plp() {
        cycle += 4;
        r.setFlags(pop());
        return cycle;
    }

    private long pha() {
        cycle += 3;
        push(r.getA());
        return cycle;
    }

    private long pla() {
        cycle += 4;
        r.setA(pop());
        setZeroByValue(r.getA());
        setNegativeByValue(r.getA());
        return cycle;
    }

    private long status(int statusType) {
        cycle += 2;
        switch (statusType) {
            case 0: //CLC
                r.clearCarry();
                break;
            case 1: //SEC
                r.setCarry();
                break;
            case 2: //CLI
                r.clearDisableInterrupt();
                break;
            case 3: //SEI
                r.setDisableInterrupt();
                break;
            case 5: //CLV
                r.clearOverflow();
                break;
            case 6: //CLD
                r.clearDecimal();
                break;
            case 7: //SED
                r.setDecimal();
                break;
            default:
        }
        return cycle;
    }

    private long jmp(int opcode) {
        cycle += 3;
        int address = m.getByte(r.getPc()) | (m.getByte(r.getPc() + 1) << 8);
        if (opcode == 0x6C) {
            cycle += 2;
            address = m.getByte(address) | (m.getByte((address & 0xFF00) | ((address + 1) & 0xFF)) << 8);
        }
        r.setPc(address);
        return cycle;
    }

    private long jsr() {
        cycle += 6;
        int nextAddress = r.getPc() + 1;
        push(nextAddress >> 8);
        push(nextAddress);
        r.setPc(m.getByte(r.getPc()) | (m.getByte(r.getPc() + 1) << 8));
        return cycle;
    }

    private long rts() {
        cycle += 6;
        int low = pop();
        int returnAddress = low | (pop() << 8);
        r.setPc(returnAddress + 1);
        return cycle;
    }

    private long brk() {
        cycle += 7;
        int interruptVector = m.getByte(0xFFFE) | (m.getByte(0xFFFF) << 8);
        int nextAddress = r.getPc() + 1;
        push(nextAddress >> 8);
        push(nextAddress);
        push(r.getFlags() | CPURegister.MASK_BREAK);
        r.setDisableInterrupt();
        r.setPc(interruptVector);
        return cycle;
    }

    private long rti() {
        cycle += 6;
        r.setFlags(pop());
        r.setBreak();
        int low = pop();
        r.setPc(low | (pop() << 8));
        return cycle;
    }

    private void push(int value) {
        setMemoryValue(0x100 | r.getSp(), value);
        r.setSp((r.getSp() - 1) & 0xFF);
    }

    private int pop() {
        r.setSp((r.getSp() + 1) & 0xFF);
        return m.getByte(0x100 | r.getSp());
    }

    private void setZeroByValue(int v) {
        r.setZero((v & 0xFF) == 0);
    }

    private void setNegativeByValue(int v) {
        r.setNegative((v & 0x80) != 0);
    }

    private void setMemoryValue(int address, int value) {
        m.setByte(address, value & 0xFF);
    }

    private int getValue1(int addressingMethod, boolean isStore) {
        if (addressingMethod == 2) {
            return immediate();
        }
        return m.getByte(getAddress1(addressingMethod, isStore));
    }

    private int getValue2(int addressingMethod, boolean isStore) {
        if (addressingMethod == 0) {
            return immediate();
        }
        return m.getByte(getAddress2(addressingMethod, isStore));
    }

    private int getValue3(int addressingMethod, boolean isStore) {
        if (addressingMethod == 0) {
            return immediate();
        }
        return m.getByte(getAddress3(addressingMethod, isStore));
    }

    // for all ALUs
    private int getAddress1(int addressingMethod, boolean isStore) {
        int arg = m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        switch (addressingMethod) {
            case 0: // Indirect, x
                return indirectX(arg);
            case 1: // Zero page
                return zeroPage(arg);
            case 3: // Absolute
                return absolute(arg);
            case 4: // Indirect, y
                return indirectY(isStore, arg);
            case 5: // Zero page, X
                return zeroPageX(arg);
            case 6: // Absolute, Y
                return absoluteY(isStore, arg);
            case 7: // Absolute, X
                return absoluteX(isStore, arg);
            default:
                r.setPc(r.getPc() - 1);
                return 0;
        }
    }

    // for STX, LDX
    private int getAddress2(int addressingMethod, boolean isStore) {
        int arg = m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        switch (addressingMethod) {
            case 1: // Zero page
                return zeroPage(arg);
            case 3: // Absolute
                return absolute(arg);
            case 5: // Zero page, Y
                return zeroPageY(arg);
            case 7: // Absolute, Y
                return absoluteY(isStore, arg);
            default:
                r.setPc(r.getPc() - 1);
                return 0;
        }
    }

    // for other
    private int getAddress3(int addressingMethod, boolean isStore) {
        int arg = m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        switch (addressingMethod) {
            case 1: // Zero page
                return zeroPage(arg);
            case 3: // Absolute
                return absolute(arg);
            case 5: // Zero page, X
                return zeroPageX(arg);
            case 7: // Absolute, X
                return absoluteX(isStore, arg);
            default:
                r.setPc(r.getPc() - 1);
                return 0;
        }
    }

    private int absoluteX(boolean isStore, int arg) {
        int arg2;
        cycle += 4;
        arg2 = m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        cycle += 4;
        int address = ((arg2 << 8) | arg) + r.getX();
        if (isStore || (address >> 8) != arg2) {
            cycle += 1;
        }
        return address;
    }

    private int absoluteY(boolean isStore, int arg) {
        int arg2;
        cycle += 4;
        arg2 = m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        cycle += 4;
        int address = ((arg2 << 8) | arg) + r.getY();
        if (isStore || (address >> 8) != arg2) {
            cycle += 1;
        }
        return address;
    }

    private int zeroPageY(int arg) {
        cycle += 4;
        return (arg + r.getY()) & 0xFF;
    }

    private int zeroPageX(int arg) {
        cycle += 4;
        return (arg + r.getX()) & 0xFF;
    }

    private int indirectX(int arg) {
        cycle += 6;
        return m.getByte((arg + r.getX()) & 0xFF) | (m.getByte((arg + r.getX() + 1) & 0xFF) << 8);
    }

    private int indirectY(boolean isStore, int arg) {
        cycle += 5;
        int high = m.getByte((arg + 1) & 0xFF);
        int address = m.getByte(arg) + (high << 8) + r.getY();
        if (isStore || (address >> 8) != high) {
            cycle += 1;
        }
        return address;
    }

    private int absolute(int arg) {
        int arg2;
        arg2 = m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        cycle += 4;
        return (arg2 << 8) | arg;
    }

    private int immediate() {
        int arg = m.getByte(r.getPc());
        r.setPc(r.getPc() + 1);
        cycle += 2;
        return arg;
    }

    private int zeroPage(int arg) {
        cycle += 3;
        return arg;
    }

    public void reset() {
        r.reset();
        r.setPc((m.getByte(0xFFFD) << 8) | m.getByte(0xFFFC));
        cycle = 0;
    }

    public void powerUp() {
        reset();
    }

    public void nmi() {
        if (RAND.nextBoolean()) {
            pendingNMI = 2;
        } else {
            nmiImpl();
        }
    }

    public void nmiImpl() {
        int interruptVector = m.getByte(0xFFFA) | (m.getByte(0xFFFB) << 8);
        push(r.getPc() >> 8);
        push(r.getPc());
        push(r.getFlags() & ~CPURegister.MASK_BREAK);
        r.setPc(interruptVector);
    }

    public void addIRQGenerator(IRQGenerator generator) {
        irqGenerators.add(generator);
    }

    private boolean checkIRQ() {
        if (r.isDisableInterrupt()) {
            return false;
        }
        for (IRQGenerator generator : irqGenerators) {
            if (generator.getIRQLevel()) {
                irq();
                return true;
            }
        }
        return false;
    }

    private void irq() {
        int interruptVector = m.getByte(0xFFFE) | (m.getByte(0xFFFF) << 8);
        push(r.getPc() >> 8);
        push(r.getPc());
        push(r.getFlags() & ~CPURegister.MASK_BREAK);
        r.setDisableInterrupt();
        r.setPc(interruptVector);
    }

    public void setMemory(Memory memory) {
        m = memory;
    }

    public Memory getMemory() {
        return m;
    }

    public CPURegister getRegister() {
        return r;
    }

    public long getCycle() {
        return cycle;
    }

    public void increaseCycle(int value) {
        cycle += value;
    }
}
