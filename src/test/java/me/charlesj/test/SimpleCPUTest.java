package me.charlesj.test;

import me.charlesj.cpu.CPURegister;
import me.charlesj.cpu.SimpleCPU;
import me.charlesj.cpu.CPU;
import me.charlesj.memory.DefaultMemory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * 2020/1/24.
 */
public class SimpleCPUTest {

    private static final Random RAND = new Random();
    public static final int TEST_COUNT = 10;

    //////////////////////////////////////////////////////////////////
    // Basic function tests
    //////////////////////////////////////////////////////////////////
    @Test
    public void testPowerUp() {
        CPU cpu = new SimpleCPU();
        TempMemory memory = initMemory(cpu);
        CPURegister register = cpu.getRegister();

        memory.setBytes(0xFFFC, new int[]{0x00, 0x80});
        cpu.powerUp();
        Assert.assertEquals("Reset Vector", 0x8000, register.getPc());
    }

    //////////////////////////////////////////////////////////////////
    // Instruction tests
    //////////////////////////////////////////////////////////////////
    @Test
    public void testAdc() {
        testAdc(127, 1, false, 128, true, true, false, false);
        testAdc(127, 0, true, 128, true, true, false, false);
        testAdc(127, -20, false, 107, false, false, false, true);
        testAdc(127, 128, true, 0, false, false, true, true);
        testAdc(128, 128, true, 1, false, true, false, true);
        testAdc(128, 128, false, 0, false, true, true, true);
        testAdc(-1, -1, false, -2, true, false, false, true);
    }

    private void testAdc(int a, int b, boolean c, int result, boolean negative, boolean overflow, boolean zero, boolean carry) {
        testCarryInstruction(0x69, "ADC Result", a, b, c, result, negative, overflow, zero, carry);
    }

    @Test
    public void testAnd() {
        testAnd(0x80, 0xF0, 0x80, true, false);
        testAnd(0x80, 0x0F, 0, false, true);
    }

    private void testAnd(int a, int b, int result, boolean negative, boolean zero) {
        testMathInstruction(0x29, "AND Result", a, b, result, negative, null, zero, null);
    }

    @Test
    public void testAsl() {
        testAsl(0x80, 0, false, true, true);
        testAsl(0x41, 0x82, true, false, false);
        testAsl(0xC1, 0x82, true, false, true);
    }

    private void testAsl(int a, int result, boolean negative, boolean zero, boolean carry) {
        testMathInstruction(0x0A, "ASL Result", a, 0, result, negative, null, zero, carry);
    }

    @Test
    public void testBcc() {
        testBranch(0x90, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearCarry();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setCarry();
            }
        });
    }

    @Test
    public void testBcs() {
        testBranch(0xB0, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setCarry();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearCarry();
            }
        });
    }

    @Test
    public void testBeq() {
        testBranch(0xF0, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setZero();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearZero();
            }
        });
    }

    @Test
    public void testBit() {
        testBit(0x84, 0x80, true, false, false);
        testBit(0xC9, 0xF2, true, true, false);
        testBit(0x71, 0xC2, true, true, false);
        testBit(0x43, 0x3C, false, false, true);
    }

    private void testBit(int a, int b, boolean m7, boolean m6, boolean zero) {
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA9, a,          // LDA #a
                0x2C, 0x05, 0x80, // BIT 8005
                b, // data: b
        }, null);
        cpu.execute();
        cpu.execute();

        CPURegister register = cpu.getRegister();
        Assert.assertEquals(a, register.getA());
        assertFlags(register, m7, m6, zero, null);
    }

    @Test
    public void testBmi() {
        testBranch(0x30, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setNegative();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearNegative();
            }
        });
    }

    @Test
    public void testBne() {
        testBranch(0xD0, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearZero();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setZero();
            }
        });
    }

    @Test
    public void testBpl() {
        testBranch(0x10, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearNegative();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setNegative();
            }
        });
    }

    @Test
    public void testBrk() {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0x00,           // BRK
                0xEA,           // NOP
                0xEA,           // NOP
                0xA9, 0x05,     // LDA #5
                0x40            // RTI
        }, mem);
        mem[0].setBytes(0xFFFE, new int[]{ 0x03, 0x80 });

        CPURegister register = cpu.getRegister();
        register.setCarry();
        register.clearDisableInterrupt();

        cpu.execute();
        cpu.execute();

        Assert.assertEquals(0x35, register.getFlags());
        Assert.assertEquals(5, register.getA());
        Assert.assertEquals(0xFD - 3, register.getSp());
        Assert.assertEquals(0x31, mem[0].getByte(0x1FB));
        Assert.assertEquals(0x02, mem[0].getByte(0x1FC));
        Assert.assertEquals(0x80, mem[0].getByte(0x1FD));

        cpu.execute();

        Assert.assertEquals(0x8002, register.getPc());
        Assert.assertEquals(0x31, register.getFlags());
        Assert.assertEquals(0xFD, register.getSp());
    }

    @Test
    public void testBvc() {
        testBranch(0x50, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearOverflow();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setOverflow();
            }
        });
    }

    @Test
    public void testBvs() {
        testBranch(0x70, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setOverflow();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearOverflow();
            }
        });
    }

    @Test
    public void testClc() {
        testModifyFlag(0x18, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setCarry();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                Assert.assertFalse(register.isCarry());
            }
        });
    }

    @Test
    public void testCld() {
        testModifyFlag(0xD8, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setDecimal();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                Assert.assertFalse(register.isDecimal());
            }
        });
    }

    @Test
    public void testCli() {
        testModifyFlag(0x58, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setDisableInterrupt();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                Assert.assertFalse(register.isDisableInterrupt());
            }
        });
    }

    @Test
    public void testClv() {
        testModifyFlag(0xB8, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.setOverflow();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                Assert.assertFalse(register.isOverflow());
            }
        });
    }

    @Test
    public void testCmp() {
        testCmp(10, 10, false, true, true);
        testCmp(255, 10, true, false, true);
        testCmp(0, 255, false, false, false);
    }

    private void testCmp(int a, int b, boolean negative, boolean zero, boolean carry) {
        testMathInstruction(0xC9, "CMP Result", a, b, a, negative, null, zero, carry);
    }

    @Test
    public void testCpx() {
        testCpx(10, 10, false, true, true);
        testCpx(255, 10, true, false, true);
        testCpx(0, 255, false, false, false);
    }

    private void testCpx(int a, int b, boolean negative, boolean zero, boolean carry) {
        testMathInstructionForX(0xE0, "CPX Result", a, b, a, negative, null, zero, carry);
    }

    @Test
    public void testCpy() {
        testCpy(10, 10, false, true, true);
        testCpy(255, 10, true, false, true);
        testCpy(0, 255, false, false, false);
    }

    private void testCpy(int a, int b, boolean negative, boolean zero, boolean carry) {
        testMathInstructionForY(0xC0, "CPY Result", a, b, a, negative, null, zero, carry);
    }

    @Test
    public void testDec() {
        testDec(0, true, false);
        testDec(1, false, true);
        testDec(128, false, false);
    }

    public void testDec(int a, boolean negative, boolean zero) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xCE, 0x03, 0x80, // DEC 8003
                a,                // data: a
        }, mem);
        cpu.execute();

        CPURegister register = cpu.getRegister();
        Assert.assertEquals((a - 1) & 0xFF, mem[0].getByte(0x8003));
        assertFlags(register, negative, null, zero, null);
    }

    @Test
    public void testDex() {
        testDex(0, true, false);
        testDex(1, false, true);
        testDex(128, false, false);
    }

    public void testDex(int a, boolean negative, boolean zero) {
        testMathInstructionForX(0xCA, "DEX Result", a, 0, (a - 1) & 0xFF, negative, null, zero, null);
    }

    @Test
    public void testDey() {
        testDey(0, true, false);
        testDey(1, false, true);
        testDey(128, false, false);
    }

    public void testDey(int a, boolean negative, boolean zero) {
        testMathInstructionForY(0x88, "DEY Result", a, 0, (a - 1) & 0xFF, negative, null, zero, null);
    }

    @Test
    public void testEor() {
        testEor(0x80, 0x70, 0xF0, true, false);
        testEor(0x80, 0x80, 0, false, true);
        testEor(0x80, 0xC0, 0x40, false, false);
    }

    private void testEor(int a, int b, int result, boolean negative, boolean zero) {
        testMathInstruction(0x49, "EOR Result", a, b, result, negative, null, zero, null);
    }

    @Test
    public void testInc() {
        testInc(254, true, false);
        testInc(255, false, true);
        testInc(127, true, false);
    }

    public void testInc(int a, boolean negative, boolean zero) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xEE, 0x03, 0x80, // INC 8003
                a,                // data: a
        }, mem);
        cpu.execute();

        CPURegister register = cpu.getRegister();
        Assert.assertEquals((a + 1) & 0xFF, mem[0].getByte(0x8003));
        assertFlags(register, negative, null, zero, null);
    }

    @Test
    public void testInx() {
        testInx(254, true, false);
        testInx(255, false, true);
        testInx(127, true, false);
    }

    public void testInx(int a, boolean negative, boolean zero) {
        testMathInstructionForX(0xE8, "INX Result", a, 0, (a + 1) & 0xFF, negative, null, zero, null);
    }

    @Test
    public void testIny() {
        testIny(254, true, false);
        testIny(255, false, true);
        testIny(127, true, false);
    }

    public void testIny(int a, boolean negative, boolean zero) {
        testMathInstructionForY(0xC8, "INY Result", a, 0, (a + 1) & 0xFF, negative, null, zero, null);
    }

    @Test
    public void testJmp() {
        {
            CPU cpu = powerUpCPUWithCode(new int[]{
                    0x4C, 0x05, 0x80, // JMP 8005
                    0xA9, 0x01,       // LDA #1
                    0xA9, 0x02,       // LDA #2
            }, null);

            CPURegister register = cpu.getRegister();
            cpu.execute();
            cpu.execute();
            Assert.assertEquals(2, register.getA());
            Assert.assertEquals(0x8007, register.getPc());
        }
        {
            TempMemory[] mem = new TempMemory[1];
            CPU cpu = powerUpCPUWithCode(new int[]{
                    0x6C, 0xFF, 0x80, // JMP (80FF)
            }, mem);
            mem[0].setByte(0x80FF, 0x00);

            CPURegister register = cpu.getRegister();
            cpu.execute();
            Assert.assertEquals(0x6C00, register.getPc());
        }
    }

    @Test
    public void testJsr() {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0x20, 0x05, 0x80, // BRK
                0xEA,             // NOP
                0xEA,             // NOP
                0xA9, 0x05,       // LDA #5
                0x60              // RTS
        }, mem);

        CPURegister register = cpu.getRegister();
        register.setCarry();
        register.clearDisableInterrupt();

        cpu.execute();
        cpu.execute();

        Assert.assertEquals(5, register.getA());
        Assert.assertEquals(0xFD - 2, register.getSp());
        Assert.assertEquals(0x02, mem[0].getByte(0x1FC));
        Assert.assertEquals(0x80, mem[0].getByte(0x1FD));

        cpu.execute();

        Assert.assertEquals(0x8003, register.getPc());
        Assert.assertEquals(0xFD, register.getSp());
    }

    @Test
    public void testLda() {
        testLda(0, false, true);
        testLda(1, false, false);
        testLda(255, true, false);
    }

    private void testLda(int a, boolean negative, boolean zero) {
        testMathInstruction(0xA9, "LDA Result", 0, a, a, negative, null, zero, null);
    }

    @Test
    public void testLdx() {
        testLdx(0, false, true);
        testLdx(1, false, false);
        testLdx(255, true, false);
    }

    private void testLdx(int a, boolean negative, boolean zero) {
        testMathInstructionForX(0xA2, "LDX Result", 0, a, a, negative, null, zero, null);
    }

    @Test
    public void testLdy() {
        testLdy(0, false, true);
        testLdy(1, false, false);
        testLdy(255, true, false);
    }

    private void testLdy(int a, boolean negative, boolean zero) {
        testMathInstructionForY(0xA0, "LDY Result", 0, a, a, negative, null, zero, null);
    }

    @Test
    public void testLsr() {
        testLsr(0x1, 0, false, true, true);
        testLsr(0x82, 0x41, false, false, false);
        testLsr(0xC1, 0x60, false, false, true);
    }

    private void testLsr(int a, int result, boolean negative, boolean zero, boolean carry) {
        testMathInstruction(0x4A, "LSR Result", a, 0, result, negative, null, zero, carry);
    }

    @Test
    public void testOra() {
        testOra(0x80, 0xF0, 0xF0, true, false);
        testOra(0x40, 0x0F, 0x4F, false, false);
        testOra(0, 0, 0, false, true);
    }

    private void testOra(int a, int b, int result, boolean negative, boolean zero) {
        testMathInstruction(0x09, "ORA Result", a, b, result, negative, null, zero, null);
    }

    @Test
    public void testPha() {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA9, 0x85,       // LDA #5
                0x48,             // PHA
                0xA9, 0x04,       // LDA #4
                0x68,             // PLA
                0xA9, 0,          // LDA #0
                0x48,             // PHA
                0x68,             // PLA
        }, mem);

        CPURegister register = cpu.getRegister();

        cpu.execute();
        cpu.execute();

        Assert.assertEquals(0xFD - 1, register.getSp());
        Assert.assertEquals(0x85, mem[0].getByte(0x1FD));

        cpu.execute();
        cpu.execute();

        Assert.assertEquals(0x85, register.getA());
        Assert.assertEquals(0xFD, register.getSp());
        assertFlags(register, true, null, false, null);

        cpu.execute();
        cpu.execute();
        cpu.execute();
        assertFlags(register, false, null, true, null);
    }

    @Test
    public void testPhp() {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA9, 0x85,       // LDA #85
                0x08,             // PHP
                0xA9, 0x0,        // LDA #0
                0x28,             // PLP
        }, mem);

        CPURegister register = cpu.getRegister();

        cpu.execute();
        cpu.execute();

        Assert.assertEquals(0xFD - 1, register.getSp());
        Assert.assertEquals(0xB4, mem[0].getByte(0x1FD));

        cpu.execute();
        cpu.execute();

        Assert.assertEquals(0xB4, register.getFlags());
        Assert.assertEquals(0xFD, register.getSp());
    }

    @Test
    public void testRol() {
        testRol(0x80, false, 0, false, true, true);
        testRol(0x41, false, 0x82, true, false, false);
        testRol(0xC1, false, 0x82, true, false, true);
        testRol(0x80, true, 1, false, false, true);
        testRol(0x41, true, 0x83, true, false, false);
        testRol(0xC1, true, 0x83, true, false, true);
    }

    private void testRol(int a, boolean setCarry, int result, boolean negative, boolean zero, boolean carry) {
        testCarryInstruction(0x2A, "ROL Result", a, 0, setCarry, result, negative, null, zero, carry);
    }

    @Test
    public void testRor() {
        testRor(0x01, false, 0, false, true, true);
        testRor(0x82, false, 0x41, false, false, false);
        testRor(0xC1, false, 0x60, false, false, true);
        testRor(0x01, true, 0x80, true, false, true);
        testRor(0x82, true, 0xC1, true, false, false);
        testRor(0xC1, true, 0xE0, true, false, true);
    }

    private void testRor(int a, boolean setCarry, int result, boolean negative, boolean zero, boolean carry) {
        testCarryInstruction(0x6A, "ROR Result", a, 0, setCarry, result, negative, null, zero, carry);
    }

    @Test
    public void testSbc() {
        testSbc(127, -1, true, 128, true, true, false, false);
        testSbc(128, 0, false, 127, false, true, false, true);
        testSbc(127, 20, true, 107, false, false, false, true);
        testSbc(128, 127, false, 0, false, true, true, true);
        testSbc(128, 128, false, -1, true, false, false, false);
        testSbc(128, 128, true, 0, false, false, true, true);
        testSbc(255, 1, true, 254, true, false, false, true);
    }

    @Test
    public void testSec() {
        testModifyFlag(0x38, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearCarry();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                Assert.assertTrue(register.isCarry());
            }
        });
    }

    @Test
    public void testSed() {
        testModifyFlag(0xF8, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearDecimal();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                Assert.assertTrue(register.isDecimal());
            }
        });
    }

    @Test
    public void testSei() {
        testModifyFlag(0x78, new RegisterRunnable() {
            public void run(CPURegister register) {
                register.clearDisableInterrupt();
            }
        }, new RegisterRunnable() {
            public void run(CPURegister register) {
                Assert.assertTrue(register.isDisableInterrupt());
            }
        });
    }

    private void testSbc(int a, int b, boolean c, int result, boolean negative, boolean overflow, boolean zero, boolean carry) {
        testCarryInstruction(0xE9, "SBC Result", a, b, c, result, negative, overflow, zero, carry);
    }

    @Test
    public void testSta() {
        for (int i = 0; i < TEST_COUNT; i++) {
            testStore(0xA9, 0x85, RAND.nextInt() & 0xFF, RAND.nextInt() & 0xFF);
        }
    }

    @Test
    public void testStx() {
        for (int i = 0; i < TEST_COUNT; i++) {
            testStore(0xA2, 0x86, RAND.nextInt() & 0xFF, RAND.nextInt() & 0xFF);
        }
    }

    @Test
    public void testSty() {
        for (int i = 0; i < TEST_COUNT; i++) {
            testStore(0xA0, 0x84, RAND.nextInt() & 0xFF, RAND.nextInt() & 0xFF);
        }
    }

    @Test
    public void testTax() {
        for (int i = 0; i < TEST_COUNT; i++) {
            int value = RAND.nextInt() & 0xFF;
            CPU cpu = testTransport(value, 0xA9, 0xAA);
            Assert.assertEquals(value, cpu.getRegister().getX());
        }
    }

    @Test
    public void testTay() {
        for (int i = 0; i < TEST_COUNT; i++) {
            int value = RAND.nextInt() & 0xFF;
            CPU cpu = testTransport(value, 0xA9, 0xA8);
            Assert.assertEquals(value, cpu.getRegister().getY());
        }
    }

    @Test
    public void testTsx() {
        CPU cpu = testTransport(0, 0xA9, 0xBA);
        Assert.assertEquals(0xFD, cpu.getRegister().getX());
    }

    @Test
    public void testTxa() {
        for (int i = 0; i < TEST_COUNT; i++) {
            int value = RAND.nextInt() & 0xFF;
            CPU cpu = testTransport(value, 0xA2, 0x8A);
            Assert.assertEquals(value, cpu.getRegister().getA());
        }
    }

    @Test
    public void testTxs() {
        for (int i = 0; i < TEST_COUNT; i++) {
            int value = RAND.nextInt() & 0xFF;
            CPU cpu = testTransport(value, 0xA2, 0x9A);
            Assert.assertEquals(value, cpu.getRegister().getSp());
        }
    }

    @Test
    public void testTya() {
        for (int i = 0; i < TEST_COUNT; i++) {
            int value = RAND.nextInt() & 0xFF;
            CPU cpu = testTransport(value, 0xA0, 0x98);
            Assert.assertEquals(value, cpu.getRegister().getA());
        }
    }

    private void testCarryInstruction(int opcode, String message, int a, int b, boolean c, int result, Boolean negative, Boolean overflow, Boolean zero, Boolean carry) {
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA9, a, // LDA #a
                opcode, b, // xxC #b
        }, null);
        cpu.execute();

        CPURegister register = cpu.getRegister();
        register.setCarry(c);
        cpu.execute();

        Assert.assertEquals(message, result & 0xFF, register.getA());
        assertFlags(register, negative, overflow, zero, carry);
    }

    private void testMathInstruction(int opcode, String message, int a, int b, int result, Boolean negative, Boolean overflow, Boolean zero, Boolean carry) {
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA9, a, // LDA #a
                opcode, b, // xxx #b
        }, null);
        cpu.execute();
        cpu.execute();

        CPURegister register = cpu.getRegister();
        Assert.assertEquals(message, result & 0xFF, register.getA());
        assertFlags(register, negative, overflow, zero, carry);
    }

    private void testMathInstructionForX(int opcode, String message, int a, int b, int result, Boolean negative, Boolean overflow, Boolean zero, Boolean carry) {
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA2, a, // LDX #a
                opcode, b, // xxx #b
        }, null);
        cpu.execute();
        cpu.execute();

        CPURegister register = cpu.getRegister();
        Assert.assertEquals(message, result & 0xFF, register.getX());
        assertFlags(register, negative, overflow, zero, carry);
    }

    private void testMathInstructionForY(int opcode, String message, int a, int b, int result, Boolean negative, Boolean overflow, Boolean zero, Boolean carry) {
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA0, a, // LDY #a
                opcode, b, // xxx #b
        }, null);
        cpu.execute();
        cpu.execute();

        CPURegister register = cpu.getRegister();
        Assert.assertEquals(message, result & 0xFF, register.getY());
        assertFlags(register, negative, overflow, zero, carry);
    }

    private void testBranch(int opcode, RegisterRunnable branchCondition, RegisterRunnable notBranchCondition) {
        {
            CPU cpu = powerUpCPUWithCode(new int[]{
                    opcode, 2, // Bxx +2
                    0xA9, 1,   // LDA #1
                    0xA9, 2    // LDA #2
            }, null);
            CPURegister register = cpu.getRegister();
            branchCondition.run(register);
            cpu.execute();
            cpu.execute();
            Assert.assertEquals(2, register.getA());
        }
        {
            CPU cpu = powerUpCPUWithCode(new int[]{
                    opcode, 2, // Bxx +2
                    0xA9, 1,   // LDA #1
                    0xA9, 2    // LDA #2
            }, null);
            CPURegister register = cpu.getRegister();
            notBranchCondition.run(register);
            cpu.execute();
            cpu.execute();
            Assert.assertEquals(1, register.getA());
        }
    }

    private void testModifyFlag(int opcode, RegisterRunnable reset, RegisterRunnable check) {
        CPU cpu = powerUpCPUWithCode(new int[]{
                opcode
        }, null);
        CPURegister register = cpu.getRegister();
        reset.run(register);
        cpu.execute();
        check.run(register);
    }

    private void testStore(int ldCode, int stCode, int value, int address) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                ldCode, value,
                stCode, address,
        }, mem);
        cpu.execute();
        cpu.execute();
        Assert.assertEquals(value, mem[0].getByte(address));
    }

    private CPU testTransport(int value, int ldCode, int transCode) {
        CPU cpu = powerUpCPUWithCode(new int[] {
                ldCode, value,       // LDA #85
                transCode,           // TAX
        }, null);
        cpu.execute();
        cpu.execute();
        return cpu;
    }

    //////////////////////////////////////////////////////////////////
    // Addressing test
    //////////////////////////////////////////////////////////////////
    @Test
    public void testZeroPage() {
        testZeroPage(0);
        testZeroPage(127);
        testZeroPage(128);
        testZeroPage(255);
    }

    private void testZeroPage(int address) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
            0xA5, address
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(address, value);
        cpu.execute();
        Assert.assertEquals(value, cpu.getRegister().getA());
    }

    @Test
    public void testZeroPageX() {
        testZeroPageX(0, 3, 3);
        testZeroPageX(127, 1, 128);
        testZeroPageX(128, 128, 0);
        testZeroPageX(255, 3, 2);
    }

    public void testZeroPageX(int address, int x, int expectedAddress) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA2, x,
                0xB5, address,
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(expectedAddress, value);
        cpu.execute();
        cpu.execute();
        Assert.assertEquals(x, cpu.getRegister().getX());
        Assert.assertEquals(value, cpu.getRegister().getA());
    }

    @Test
    public void testZeroPageY() {
        testZeroPageY(0, 3, 3);
        testZeroPageY(127, 1, 128);
        testZeroPageY(128, 128, 0);
        testZeroPageY(255, 3, 2);
    }

    public void testZeroPageY(int address, int y, int expectedAddress) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA0, y,
                0xB6, address,
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(expectedAddress, value);
        cpu.execute();
        cpu.execute();
        Assert.assertEquals(y, cpu.getRegister().getY());
        Assert.assertEquals(value, cpu.getRegister().getX());
    }

    @Test
    public void testAbsolute() {
        testAbsolute(0);
        testAbsolute(127);
        testAbsolute(128);
        testAbsolute(255);
        testAbsolute(0x1320);
        testAbsolute(0x1000);
        testAbsolute(0x13FF);
        testAbsolute(0x1F10);
    }

    private void testAbsolute(int address) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xAD, (address & 0xFF), (address >> 8)
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(address, value);
        cpu.execute();
        Assert.assertEquals(value, cpu.getRegister().getA());
    }

    @Test
    public void testAbsoluteX() {
        testAbsoluteX(0, 20, 20);
        testAbsoluteX(127, 10, 137);
        testAbsoluteX(128, 5, 133);
        testAbsoluteX(255, 10, 265);
        testAbsoluteX(0x1320, 0xFF, 0x141F);
        testAbsoluteX(0x1000, 1, 0x1001);
        testAbsoluteX(0x13FF, 3, 0x1402);
        testAbsoluteX(0x1F10, 16, 0x1F20);
    }

    private void testAbsoluteX(int address, int x, int expectedAddress) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA2, x,
                0xBD, (address & 0xFF), (address >> 8)
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(expectedAddress, value);
        cpu.execute();
        cpu.execute();
        Assert.assertEquals(x, cpu.getRegister().getX());
        Assert.assertEquals(value, cpu.getRegister().getA());
    }

    @Test
    public void testAbsoluteY() {
        testAbsoluteY(0, 20, 20);
        testAbsoluteY(127, 10, 137);
        testAbsoluteY(128, 5, 133);
        testAbsoluteY(255, 10, 265);
        testAbsoluteY(0x1320, 0xFF, 0x141F);
        testAbsoluteY(0x1000, 1, 0x1001);
        testAbsoluteY(0x13FF, 3, 0x1402);
        testAbsoluteY(0x1F10, 16, 0x1F20);
    }

    private void testAbsoluteY(int address, int y, int expectedAddress) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA0, y,
                0xB9, (address & 0xFF), (address >> 8)
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(expectedAddress, value);
        cpu.execute();
        cpu.execute();
        Assert.assertEquals(y, cpu.getRegister().getY());
        Assert.assertEquals(value, cpu.getRegister().getA());
    }

    @Test
    public void testIndirectX() {
        testIndirectX(0, 1, 1, 2, 0x1FFF);
        testIndirectX(255, 0, 255, 0, 0xB00);
        testIndirectX(128, 127, 255, 0, 0xB12);
        testIndirectX(128, 128, 0, 1, 0x109);
    }

    public void testIndirectX(int address, int x, int low, int high, int expectedAddress) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA2, x,
                0xA1, address,
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(low, expectedAddress & 0xFF);
        mem[0].setByte(high, expectedAddress >> 8);
        mem[0].setByte(expectedAddress, value);
        cpu.execute();
        cpu.execute();
        Assert.assertEquals(x, cpu.getRegister().getX());
        Assert.assertEquals(value, cpu.getRegister().getA());
    }

    @Test
    public void testIndirectY() {
        testIndirectY(0, 10, 0, 1, 0x1FF, 0x1FF + 10);
        testIndirectY(255, 10, 255, 0, 0x100, 0x10A);
        testIndirectY(127, 255, 127, 128, 0x301, 0x400);
    }

    public void testIndirectY(int address, int y, int low, int high, int expectedAddress, int expectedAddress2) {
        TempMemory[] mem = new TempMemory[1];
        CPU cpu = powerUpCPUWithCode(new int[] {
                0xA0, y,
                0xB1, address,
        }, mem);
        int value = RAND.nextInt() & 0xFF;
        mem[0].setByte(low, expectedAddress & 0xFF);
        mem[0].setByte(high, expectedAddress >> 8);
        mem[0].setByte(expectedAddress2, value);
        cpu.execute();
        cpu.execute();
        Assert.assertEquals(y, cpu.getRegister().getY());
        Assert.assertEquals(value, cpu.getRegister().getA());
    }

    //////////////////////////////////////////////////////////////////
    // Utilities
    //////////////////////////////////////////////////////////////////
    interface RegisterRunnable {
        void run(CPURegister register);
    }

    private void assertFlags(CPURegister register, Boolean negative, Boolean overflow, Boolean zero, Boolean carry) {
        if (overflow != null) {
            Assert.assertEquals("Overflow Flag", overflow, register.isOverflow());
        }
        if (negative != null) {
            Assert.assertEquals("Negative Flag", negative, register.isNegative());
        }
        if (carry != null) {
            Assert.assertEquals("Carry Flag", carry, register.isCarry());
        }
        if (zero != null) {
            Assert.assertEquals("Zero Flag", zero, register.isZero());
        }
    }

    private TempMemory initMemory(CPU cpu) {
        TempMemory memory = new TempMemory(0x10000);
        cpu.setMemory(memory);
        return memory;
    }

    private CPU powerUpCPUWithCode(int[] code, TempMemory[] memoryList) {
        CPU cpu = new SimpleCPU();
        TempMemory memory = initMemory(cpu);
        memory.setBytes(0xFFFC, new int[]{0x00, 0x80});
        memory.setBytes(0x8000, code);
        if (memoryList != null) {
            memoryList[0] = memory;
        }
        cpu.powerUp();
        return cpu;
    }

    class TempMemory extends DefaultMemory {
        public TempMemory(int size) {
            super(size);
        }
        public void setBytes(int address, int[] data) {
            for (int i=0; i<data.length; i++) {
                super.setByte(address + i, data[i]);
            }
        }
    }
}
