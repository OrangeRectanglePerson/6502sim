package MainComComponents;

import Extras.UnsignedMath;

import java.lang.reflect.Method;
import java.util.function.Function;

public class CPU{

    //internal registers
    private short programCounter = 0x0000;
    private byte a = 0x00; //accumulator
    private byte x = 0x00; //x register
    private byte y = 0x00; //y register
    private byte stackPointer = 0x00;
    private byte stat_regs = 0x00;


    // Variables for emulation
    private short addr_abs = 0x0000; // memory address to fetch a value from
    private byte  fetched = 0x00;   // opcode param value
    private short addr_rel = 0x00;   // Represents address offset going from branch instruction [-128,127]
    private byte  opcode = 0x00;   // Is the current instruction byte
    private byte  cycles = 0;	   // Counts how many cycles current instruction has remaining
    private int clock_count = 0;	// Accumulation of the number of clocks
    private short temp = 0x0000; // temporary variable for stuff
    private boolean isIMP = false; //is addressing mode IMPlied?

    public CPU(){

    }

    //READ & WRITE FUNCTIONS
    //cpu takes data fom an address on the bus
    //e.g. reading opcodes from ROM or reading data from RAM
    public byte activelyRead(short receiveAdr) { return Bus.serveDataFromAdr(receiveAdr); }
    //cpu writes data to an address on the bus
    //e.g. writing data to RAM, writing graphics to VRAM, setting flags in sound chip
    public void activelyWrite(short requestAdr, byte data) {
        Bus.serveDataToAdr(requestAdr,data);
    }

    //data cannot be forcefully read from or written into the cpu


    //FLAG FUNCTIONS
    private byte getFlag(CPUFlags f){
        //return 1 if flag is set, 0 if flag is not set
        return((stat_regs & f.getPosition()) == f.getPosition()) ? (byte)1 : (byte)0;
    }

    private void setFlag(CPUFlags f, boolean setTo){
        if(setTo){
            //if setting flag to yes,
            //OR status register with flag position
            //For flags not being set: 0 OR 0 = 0 | 1 OR 0 = 1
            //FOr flag being set: 0 OR 1 = 1 | 1 OR 1 = 1
            stat_regs |= f.getPosition();
        }
        else {
            //if setting flag to no,
            //AND status register with unary NOT of flag position
            //For flags not being set: 0 AND 1 = 0 | 1 AND 1 = 1
            //FOr flag being set: 0 AND 0 = 0 | 1 AND 0 = 0
            stat_regs &= ~f.getPosition();
        }
    }

    //CLOCK, INTERRUPT & RESET

    public void clock() {
        //here we define what happens in 1 clock cycle

        /* we will use the cycles variable here
         * the cycles variable keeps track of how many cycles the current instruction needs left
         * when the cycles > 0, this means in real life, the processor is still processing the instruction on this tick
         * in the emulator, we will emulate this by doing nothing but decrementing the clock variable when clock > 0
         * else, lets do the monstrosity that is the 150+ rung ifelse ladder
         *
         * the anatomy of an instruction-executing clock cycle:
         * 1) read in an instruction @ current program counter
         * 2) increment program counter (move to next addr)
         * 3) set number of cycles needed
         * 4) set/execute/choose addressing mode (store returned byte in variable "extraCycleA")
         * 5) perform operation (store returned byte in variable "extraCycleB")
         * 6) if both extraCycleA & B are 1 then add additional cycle to cycle count
         *
         * [3, 4 & 5 will be painful because there are 150+ possible combinations and i don't think arrays of
         * method references exist in java]
         *
         * 2 things always to do when running a clock cycle (at the end):
         * 1) decrease number of clock cycles left for current instruction
         * 2) increase clock count variable (it stores total num of clocks the cpu has run)
         */

        byte extraCycleA, extraCycleB;
        if(cycles <= 0){
            //1
            opcode = activelyRead(programCounter);

            //2
            programCounter++;

            //3, 4 & 5

            //high nibble is 0x0
            //00 brk imm 7
            if ((opcode & 0xff) == 0x00){
                extraCycleA = IMP();
                extraCycleB = BRK();
                cycles = 7;
            }
            //01 ora izx 6
            else if ((opcode & 0xff) == 0x01) {
                extraCycleA = IZX();
                extraCycleB = ORA();
                cycles = 6;
            }
            //05 ora zp0 3
            else if ((opcode & 0xff) == 0x05) {
                extraCycleA = ZP0();
                extraCycleB = ORA();
                cycles = 3;
            }
            //06 asl zp0 5
            else if ((opcode & 0xff) == 0x06) {
                extraCycleA = ZP0();
                extraCycleB = ASL();
                cycles = 5;
            }
            //08 php imp 3
            else if ((opcode & 0xff) == 0x08) {
                extraCycleA = IMP();
                extraCycleB = PHP();
                cycles = 3;
            }
            //09 ora imm 2
            else if ((opcode & 0xff) == 0x09) {
                extraCycleA = IMM();
                extraCycleB = ORA();
                cycles = 2;
            }
            //0a asl imp 2
            else if ((opcode & 0xff) == 0x0A) {
                extraCycleA = IMP();
                extraCycleB = ASL();
                cycles = 2;
            }
            //0d ora abs 4
            else if ((opcode & 0xff) == 0x0D) {
                extraCycleA = ABS();
                extraCycleB = ORA();
                cycles = 4;
            }
            //0e asl abs 6
            else if ((opcode & 0xff) == 0x0E) {
                extraCycleA = ABS();
                extraCycleB = ASL();
                cycles = 6;
            }

            //high nibble is 0x1
            //10 bpl rel 2
            else if ((opcode & 0xff) == 0x10) {
                extraCycleA = REL();
                extraCycleB = BPL();
                cycles = 2;
            }
            //11 ora izy 5
            else if ((opcode & 0xff) == 0x11) {
                extraCycleA = IZY();
                extraCycleB = ORA();
                cycles = 5;
            }
            //15 ora zpx 4
            else if ((opcode & 0xff) == 0x15) {
                extraCycleA = ZPX();
                extraCycleB = ORA();
                cycles = 4;
            }
            //16 asl zpx 6
            else if ((opcode & 0xff) == 0x16) {
                extraCycleA = ZPX();
                extraCycleB = ASL();
                cycles = 6;
            }
            //18 clc imp 2
            else if ((opcode & 0xff) == 0x18) {
                extraCycleA = IMP();
                extraCycleB = CLC();
                cycles = 2;
            }
            //19 ora aby 4
            else if ((opcode & 0xff) == 0x19) {
                extraCycleA = ABY();
                extraCycleB = ORA();
                cycles = 4;
            }
            //1d ora abx 4
            else if ((opcode & 0xff) == 0x1D) {
                extraCycleA = ABX();
                extraCycleB = ORA();
                cycles = 4;
            }
            //1e asl abx 7
            else if ((opcode & 0xff) == 0x1E) {
                extraCycleA = ABX();
                extraCycleB = ASL();
                cycles = 7;
            }

            //high nibble 0x2
            //20 jsr abs 6
            else if ((opcode & 0xff) == 0x20) {
                extraCycleA = ABS();
                extraCycleB = JSR();
                cycles = 6;
            }
            //21 and izx 6
            else if ((opcode & 0xff) == 0x21) {
                extraCycleA = IZX();
                extraCycleB = AND();
                cycles = 6;
            }
            //24 bit zp0 3
            else if ((opcode & 0xff) == 0x24) {
                extraCycleA = ZP0();
                extraCycleB = BIT();
                cycles = 3;
            }
            //25 and zpo 3
            else if ((opcode & 0xff) == 0x25) {
                extraCycleA = ZP0();
                extraCycleB = AND();
                cycles = 3;
            }
            //26 rol zp0 5
            else if ((opcode & 0xff) == 0x26) {
                extraCycleA = ZP0();
                extraCycleB = ROL();
                cycles = 5;
            }
            //28 plp imp 4
            else if ((opcode & 0xff) == 0x28) {
                extraCycleA = IMP();
                extraCycleB = PLP();
                cycles = 4;
            }
            //29 and imm 2
            else if ((opcode & 0xff) == 0x29) {
                extraCycleA = IMM();
                extraCycleB = AND();
                cycles = 2;
            }
            //2a rol imp 2
            else if ((opcode & 0xff) == 0x2A) {
                extraCycleA = IMP();
                extraCycleB = ROL();
                cycles = 2;
            }
            //2c bit abs 4
            else if ((opcode & 0xff) == 0x2C) {
                extraCycleA = ABS();
                extraCycleB = BIT();
                cycles = 4;
            }
            //2d and abs 4
            else if ((opcode & 0xff) == 0x2D) {
                extraCycleA = ABS();
                extraCycleB = AND();
                cycles = 4;
            }
            //2e rol abs 6
            else if ((opcode & 0xff) == 0x2E) {
                extraCycleA = ABS();
                extraCycleB = ROL();
                cycles = 6;
            }

            //high nibble 0x3
            //30 bmi rel 2
            else if ((opcode & 0xff) == 0x30) {
                extraCycleA = REL();
                extraCycleB = BMI();
                cycles = 2;
            }
            //31 and izy 5
            else if ((opcode & 0xff) == 0x31) {
                extraCycleA = IZY();
                extraCycleB = AND();
                cycles = 5;
            }
            //35 and zpx 4
            else if ((opcode & 0xff) == 0x35) {
                extraCycleA = ZPX();
                extraCycleB = AND();
                cycles = 4;
            }
            //36 rol zpx 6
            else if ((opcode & 0xff) == 0x36) {
                extraCycleA = ZPX();
                extraCycleB = ROL();
                cycles = 6;
            }
            //38 sec imp 2
            else if ((opcode & 0xff) == 0x38) {
                extraCycleA = IMP();
                extraCycleB = SEC();
                cycles = 2;
            }
            //39 and abx 4
            else if ((opcode & 0xff) == 0x39) {
                extraCycleA = ABX();
                extraCycleB = AND();
                cycles = 4;
            }
            //3d and abx 4
            else if ((opcode & 0xff) == 0x3D) {
                extraCycleA = ABX();
                extraCycleB = AND();
                cycles = 4;
            }
            //3e rol abx 7
            else if ((opcode & 0xff) == 0x3E) {
                extraCycleA = ABX();
                extraCycleB = ROL();
                cycles = 7;
            }

            //high nibble 0x4
            //40 rti imp 6
            else if ((opcode & 0xff) == 0x40) {
                extraCycleA = IMP();
                extraCycleB = RTI();
                cycles = 6;
            }
            //41 eor izx 6
            else if ((opcode & 0xff) == 0x41) {
                extraCycleA = IZX();
                extraCycleB = EOR();
                cycles = 6;
            }
            //45 eor zp0 3
            else if ((opcode & 0xff) == 0x45) {
                extraCycleA = ZP0();
                extraCycleB = EOR();
                cycles = 3;
            }
            //46 lsr zp0 5
            else if ((opcode & 0xff) == 0x46) {
                extraCycleA = ZP0();
                extraCycleB = LSR();
                cycles = 5;
            }
            //48 pha imp 3
            else if ((opcode & 0xff) == 0x48) {
                extraCycleA = IMP();
                extraCycleB = PHA();
                cycles = 3;
            }
            //49 eor imm 2
            else if ((opcode & 0xff) == 0x49) {
                extraCycleA = IMM();
                extraCycleB = EOR();
                cycles = 2;
            }
            //4a lsr imp 2
            else if ((opcode & 0xff) == 0x4A) {
                extraCycleA = IMP();
                extraCycleB = LSR();
                cycles = 2;
            }
            //4c jmp abs 3
            else if ((opcode & 0xff) == 0x4C) {
                extraCycleA = ABS();
                extraCycleB = JMP();
                cycles = 3;
            }
            //4d eor abs 4
            else if ((opcode & 0xff) == 0x4D) {
                extraCycleA = ABS();
                extraCycleB = EOR();
                cycles = 4;
            }
            //4e lsr abs 6
            else if ((opcode & 0xff) == 0x4E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }

            //high nibble 0x5
            //50 bvc rel 2
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }
            //51 eor izy 5
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }
            //55 eor zpx 4
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }
            //56 lsr zpx 6
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }
            //58 cli imp 2
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }
            //59 eor aby 4
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }
            //5d eor abx 4
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }
            //5e lsr abx 7
            else if ((opcode & 0xff) == 0x5E) {
                extraCycleA = ABS();
                extraCycleB = LSR();
                cycles = 6;
            }

            //high nibble 0x6
            //60 rts imp 6
            else if ((opcode & 0xff) == 0x60) {
                extraCycleA = IMP();
                extraCycleB = RTS();
                cycles = 6;
            }
            //61 adc izx 6
            else if ((opcode & 0xff) == 0x61) {
                extraCycleA = IZX();
                extraCycleB = ADC();
                cycles = 6;
            }
            //65 adc zp0 3
            else if ((opcode & 0xff) == 0x65) {
                extraCycleA = ZP0();
                extraCycleB = ADC();
                cycles = 3;
            }
            //66 ror zp0 5
            else if ((opcode & 0xff) == 0x66) {
                extraCycleA = ZP0();
                extraCycleB = ROR();
                cycles = 5;
            }
            //68 pla imp 4
            else if ((opcode & 0xff) == 0x68) {
                extraCycleA = IMP();
                extraCycleB = PLA();
                cycles = 4;
            }
            //69 adc imm 2
            else if ((opcode & 0xff) == 0x69) {
                extraCycleA = IMM();
                extraCycleB = ADC();
                cycles = 2;
            }
            //6a ror imp 2
            else if ((opcode & 0xff) == 0x6A) {
                extraCycleA = IMP();
                extraCycleB = ROR();
                cycles = 2;
            }
            //6c jmp ind 5
            else if ((opcode & 0xff) == 0x6C) {
                extraCycleA = IND();
                extraCycleB = JMP();
                cycles = 5;
            }
            //6d adc abs 4
            else if ((opcode & 0xff) == 0x6D) {
                extraCycleA = ABS();
                extraCycleB = ADC();
                cycles = 4;
            }
            //6e ror abs 6
            else if ((opcode & 0xff) == 0x6E) {
                extraCycleA = ABS();
                extraCycleB = ROR();
                cycles = 6;
            }

            //high nibble 0x7
            //70 bvs rel 2
            else if ((opcode & 0xff) == 0x70) {
                extraCycleA = REL();
                extraCycleB = BVS();
                cycles = 2;
            }
            //71 adc izy 5
            else if ((opcode & 0xff) == 0x71) {
                extraCycleA = IZY();
                extraCycleB = ADC();
                cycles = 5;
            }
            //75 adc zpx 4
            else if ((opcode & 0xff) == 0x75) {
                extraCycleA = ZPX();
                extraCycleB = ADC();
                cycles = 4;
            }
            //76 ror zpx 6
            else if ((opcode & 0xff) == 0x76) {
                extraCycleA = ZPX();
                extraCycleB = ROR();
                cycles = 6;
            }
            //78 sei imp 2
            else if ((opcode & 0xff) == 0x78) {
                extraCycleA = IMP();
                extraCycleB = SEI();
                cycles = 2;
            }
            //79 adc aby 4
            else if ((opcode & 0xff) == 0x79) {
                extraCycleA = ABY();
                extraCycleB = ADC();
                cycles = 4;
            }
            //7d adc abx 4
            else if ((opcode & 0xff) == 0x7D) {
                extraCycleA = ABX();
                extraCycleB = ADC();
                cycles = 4;
            }
            //7e ror abx 7
            else if ((opcode & 0xff) == 0x7E) {
                extraCycleA = ABX();
                extraCycleB = ROR();
                cycles = 7;
            }

            //high nibble 0x8
            //81 sta izx 6
            else if ((opcode & 0xff) == 0x81) {
                extraCycleA = IZX();
                extraCycleB = STA();
                cycles = 6;
            }
            //84 sty zp0 3
            else if ((opcode & 0xff) == 0x84) {
                extraCycleA = ZP0();
                extraCycleB = STY();
                cycles = 3;
            }
            //85 sta zp0 3
            else if ((opcode & 0xff) == 0x85) {
                extraCycleA = ZP0();
                extraCycleB = STA();
                cycles = 3;
            }
            //86 stx zp0 3
            else if ((opcode & 0xff) == 0x86) {
                extraCycleA = ZP0();
                extraCycleB = STX();
                cycles = 3;
            }
            //88 dey imp 2
            else if ((opcode & 0xff) == 0x88) {
                extraCycleA = IMP();
                extraCycleB = DEY();
                cycles = 2;
            }
            //8a txa imp 2
            else if ((opcode & 0xff) == 0x8A) {
                extraCycleA = IMP();
                extraCycleB = TXA();
                cycles = 2;
            }
            //8c sty abs 4
            else if ((opcode & 0xff) == 0x8C) {
                extraCycleA = ABS();
                extraCycleB = STY();
                cycles = 4;
            }
            //8d sta abs 4
            else if ((opcode & 0xff) == 0x8D) {
                extraCycleA = ABS();
                extraCycleB = STA();
                cycles = 4;
            }
            //8e stx abs 4
            else if ((opcode & 0xff) == 0x8E) {
                extraCycleA = ABS();
                extraCycleB = STX();
                cycles = 4;
            }

            //high nibble 0x9
            //90 bcc rel 2
            else if ((opcode & 0xff) == 0x90) {
                extraCycleA = REL();
                extraCycleB = BCC();
                cycles = 2;
            }
            //91 sta izy 6
            else if ((opcode & 0xff) == 0x91) {
                extraCycleA = IZY();
                extraCycleB = STA();
                cycles = 6;
            }
            //94 sty zpx 4
            else if ((opcode & 0xff) == 0x94) {
                extraCycleA = ZPX();
                extraCycleB = STY();
                cycles = 4;
            }
            //95 sta zpx 4
            else if ((opcode & 0xff) == 0x95) {
                extraCycleA = ZPX();
                extraCycleB = STA();
                cycles = 4;
            }
            //96 stx zpy 4
            else if ((opcode & 0xff) == 0x96) {
                extraCycleA = ZPY();
                extraCycleB = STX();
                cycles = 4;
            }
            //98 tya imp 2
            else if ((opcode & 0xff) == 0x98) {
                extraCycleA = IMP();
                extraCycleB = TYA();
                cycles = 2;
            }
            //99 sta aby 5
            else if ((opcode & 0xff) == 0x99) {
                extraCycleA = ABY();
                extraCycleB = STA();
                cycles = 5;
            }
            //9a txs imp 2
            else if ((opcode & 0xff) == 0x9A) {
                extraCycleA = IMP();
                extraCycleB = TXS();
                cycles = 2;
            }
            //9d sta abx 5
            else if ((opcode & 0xff) == 0x9D) {
                extraCycleA = ABX();
                extraCycleB = STA();
                cycles = 5;
            }

            //high nibble 0xa
            //a0 ldy imm 2
            else if ((opcode & 0xff) == 0xA0) {
                extraCycleA = IMM();
                extraCycleB = LDY();
                cycles = 2;
            }
            //a1 lda izx 6
            else if ((opcode & 0xff) == 0xA1) {
                extraCycleA = IZX();
                extraCycleB = LDA();
                cycles = 2;
            }
            //a2 ldx imm 2
            else if ((opcode & 0xff) == 0xA2) {
                extraCycleA = IMM();
                extraCycleB = LDX();
                cycles = 2;
            }
            //a4 ldy zp0 3
            else if ((opcode & 0xff) == 0xA4) {
                extraCycleA = ZP0();
                extraCycleB = LDY();
                cycles = 3;
            }
            //a5 lda zp0 3
            else if ((opcode & 0xff) == 0xA5) {
                extraCycleA = ZP0();
                extraCycleB = LDA();
                cycles = 3;
            }
            //a6 ldx zp0 3
            else if ((opcode & 0xff) == 0xA6) {
                extraCycleA = ZP0();
                extraCycleB = LDX();
                cycles = 3;
            }
            //a8 tay imp 2
            else if ((opcode & 0xff) == 0xA8) {
                extraCycleA = IMP();
                extraCycleB = TAY();
                cycles = 2;
            }
            //a9 lda imm 2
            else if ((opcode & 0xff) == 0xA9) {
                extraCycleA = IMM();
                extraCycleB = LDA();
                cycles = 2;
            }
            //aa tax imp 2
            else if ((opcode & 0xff) == 0xAA) {
                extraCycleA = IMP();
                extraCycleB = TAX();
                cycles = 2;
            }
            //ac ldy abs 4
            else if ((opcode & 0xff) == 0xAC) {
                extraCycleA = ABS();
                extraCycleB = LDY();
                cycles = 4;
            }
            //ad lda abs 4
            else if ((opcode & 0xff) == 0xAD) {
                extraCycleA = ABS();
                extraCycleB = LDA();
                cycles = 4;
            }
            //ae ldx abs 4
            else if ((opcode & 0xff) == 0xAE) {
                extraCycleA = ABS();
                extraCycleB = LDX();
                cycles = 42;
            }

            //high nibble 0xb
            //b0 bcs rel 2
            //b1 lda izy 5
            //b4 ldy zpx 4
            //b5 lda zpx 4
            //b6 ldx zpy 4
            //b8 clv imp 2
            //b9 lda aby 4
            //ba tsx imp 2
            //bc ldy abx 4
            //bd lda abx 4
            //be ldx aby 4

            //high nibble 0xc
            //c0 cpy imm 2
            //c1 cmp izx 6
            //c4 cpy zp0 3
            //c5 cmp zp0 3
            //c6 dec zp0 5
            //c8 iny imp 2
            //c9 cmp imm 2
            //ca dex imp 2
            //cc cpy abs 4
            //cd cmp abs 4
            //ce dec abs 6

            //high nibble 0xd
            //d0 bne rel 2
            //d1 cmp izy 5
            //d5 cmp zpx 4
            //d6 dec zpx 6
            //d8 cld imp 2
            //d9 cmp aby 4
            //dd cmp abx 4
            //de dec abx 7

            //high nibble 0xe
            //e0 cpx imm 2
            //e1 sbc izx 6
            //e4 cpx zp0 3
            //e5 sbc zp0 3
            //e6 inc zp0 5
            //e8 inx imp 2
            //e9 sbc imm 2
            //ea nop imp 2
            //ec cpx abs 4
            //ed sbc abs 4
            //ee inc abs 6

            //hgh nibble 0xf
            //f0 beq rel 2
            //f1 sbc izy 5
            //f5 sbc zpx 4
            //f6 inc zpx 6
            //f8 sed imp 2
            //f9 sbc aby 4
            //fd sbc abx 4
            //fe inc abx 7

        }


    }

    //ADDRESSING MODES
    //methods here wil return bytes
    //bytes are how many extra clock cycles are needed by adressing mode

    //Implied
    //no input is needed by the instruction (e.g. set status bit)
    //fetch A ofr instruction like PHA
    private byte IMP(){
        isIMP = true;
        fetched = a;
        return 0;
    }

    //Immediate
    //fetch the value after the instruction byte
    //load address of this value into addr_abs
    //then advance program counter to next address
    private byte IMM() {
        isIMP = false;
        addr_abs = programCounter++;
        return 0;
    }

    //Zero Page
    //Access adress 0x0000 to 0x00FF (this means only 1 byte is needed to be fetched)
    //(this exists to save ROM space)
    private byte ZP0(){
        isIMP = false;
        addr_abs = activelyRead(programCounter);
        programCounter++;
        addr_abs &= 0x00ff; // make sure high byte is empty
        return 0;
    }

    //Zero Page X Offset
    //Like Zero Page but the value in X is added to address read
    //(Accessible address range is still only 0x0000 to 0x00FF)
    private byte ZPX(){
        isIMP = false;
        //do unsigned addition of x and byte at pc
        addr_abs = UnsignedMath.addByte(activelyRead(programCounter),x);
        programCounter++;
        addr_abs &= 0x00ff; // make sure high byte is empty
        return 0;
    }

    //Zero Page Y Offset
    //Like Zero Page but the value in Y is added to address read
    //(Accessible address range is still only 0x0000 to 0x00FF)
    private byte ZPY(){
        isIMP = false;
        //do unsigned addition of y and byte at pc
        addr_abs = UnsignedMath.addByte(activelyRead(programCounter),y);
        programCounter++;
        addr_abs &= 0x00ff; // make sure high byte is empty
        return 0;
    }

    //Relative
    //This address mode is for branch instructions.
    //The address must reside within -128 to +127 of the branch instruction.
    private byte REL(){
        isIMP = false;
        addr_rel = activelyRead(programCounter);
        programCounter++;
        if((addr_rel & 0x8000) == 0x8000){
            addr_rel |= 0xff00;
        }
        return 0;
    }

    //Absolute
    //next two bites are read in order of low->high
    private byte ABS(){
        isIMP = false;
        byte low = activelyRead(programCounter);
        programCounter++;
        byte high = activelyRead(programCounter);
        programCounter++;
        addr_abs = UnsignedMath.byteToShort(high,low); //shift high byte up 8 bits then or in the low byte
        return 0;
    }

    //Absolute with x offset
    //absolute adressing but add X to the address
    //if addition chacnges the high byte then add 1 clock cycle
    private byte ABX(){
        isIMP = false;
        byte low = activelyRead(programCounter);
        programCounter++;
        byte high = activelyRead(programCounter);
        programCounter++;
        addr_abs = UnsignedMath.byteToShort(high,low); //shift high byte up 8 bits then or in the low byte
        addr_abs = UnsignedMath.addShort(addr_abs,x);
        if((addr_abs & 0xff00) != UnsignedMath.byteToShort(high,(byte)0)) return 1;
        else return 0;
    }

    //absolute with y offset
    private byte ABY(){
        isIMP = false;
        byte low = activelyRead(programCounter);
        programCounter++;
        byte high = activelyRead(programCounter);
        programCounter++;
        addr_abs = UnsignedMath.byteToShort(high,low); //shift high byte up 8 bits then or in the low byte
        addr_abs = UnsignedMath.addShort(addr_abs,y);
        if((addr_abs & 0xff00) != UnsignedMath.byteToShort(high,(byte)0)) return 1;
        else return 0;
    }

    //indirect addressing [pointers]
    //The supplied 16-bit address is read to get the actual 16-bit address
    //To be accurate, a bug has to be emulated:
    //If the low byte of the supplied address is 0xFF,
    //high byte is read from 0xXX00 where XX is high byte of pointer
    private byte IND(){
        isIMP = false;
        byte ptr_low = activelyRead(programCounter);
        programCounter++;
        byte ptr_high = activelyRead(programCounter);
        programCounter++;

        short ptr = UnsignedMath.byteToShort(ptr_high,ptr_low);

        if(ptr_low == (byte)0xff){ //emulate the bug
            addr_abs = (short)((activelyRead((short) (ptr & (short)0xff00)) << 8) | activelyRead(ptr));
        }
        else { //"normal" functionality
            addr_abs = (short)((activelyRead((short) (ptr + 1)) << 8) | activelyRead(ptr));
        }
        return 0;
    }

    //indirect (zero page x)
    //The supplied 8-bit address is offset by X Register to index a location with high byte 0x00
    //"actual" 2 byte addr is then read from there
    private byte IZX(){
        isIMP = false;
        short zp_ptr = UnsignedMath.byteToShort(activelyRead(programCounter));
        programCounter++;

        short x_short = UnsignedMath.byteToShort(x);

        short pointer_x = UnsignedMath.addShort(zp_ptr,x_short);

        byte low = activelyRead((short)(pointer_x & 0x00ff));
        byte high = activelyRead((short)(UnsignedMath.addShort(pointer_x,(short)1) & (short)0x00ff));

        addr_abs = UnsignedMath.byteToShort(high,low);

        return 0;
    }

    //(Indirect zero page) Y
    //The supplied 8-bit address indexes a location in page 0x00
    //Y register is added to adress at 0x00XX
    //if offset causes high byte to change, extra clock cycle needed
    private byte IZY(){
        isIMP = false;
        short zp_ptr = UnsignedMath.byteToShort(activelyRead(programCounter));
        programCounter++;

        byte low = activelyRead(zp_ptr);
        byte high = activelyRead(UnsignedMath.addShort(zp_ptr,(short)1));

        addr_abs = UnsignedMath.byteToShort(high,low);
        addr_abs = UnsignedMath.addShort(addr_abs,UnsignedMath.byteToShort(y));

        if((addr_abs & 0xff00) != UnsignedMath.byteToShort(high,(byte)0)) return 1;
        else return 0;
    }

    //fetch the data from the address into fetched
    private void fetch(){
        //fetch only if addressing mode is NOT IMPlied
        if(!isIMP) fetched = Bus.serveDataFromAdr(addr_abs);
    }

    //OPCODES

    // some basics of stuff:
    // to check the trueness of a bit in a number, just & the bit with 1.
    // if bit is 1, out is 1
    // if bit is 0, out is 1

    // Addition w/ carry in
    // Function:    A = A + M + C
    // Flags Out:   C, V, N, Z
    // This function is to add a value to the accumulator and a carry bit.
    // If the result is > 255 there is an overflow setting the carry bit.
    //
    // In principle under the -128 to 127 range:
    // 10000000 = -128, 11111111 = -1, 00000000 = 0, 00000001 = +1, 01111111 = +127
    //
    // Overflow possibilities hypothesis
    // Positive Number + Positive Number = Negative Result -> Overflow
    // Negative Number + Negative Number = Positive Result -> Overflow
    // Positive Number + Negative Number = Either Result -> Cannot Overflow
    // Positive Number + Positive Number = Positive Result -> OK! No Overflow
    // Negative Number + Negative Number = Negative Result -> OK! NO Overflow
    // NOTE!
    // THE OVERFLOW FLAG IS FOR OVERFLOW IF TEH ADDITION WAS SIGNED
    private byte ADC(){
        //fetch data
        fetch();

        //add bytes from accumulator, fetched and carry flag
        temp = (short) (Byte.toUnsignedInt(a) + Byte.toUnsignedInt(fetched) + Byte.toUnsignedInt(getFlag(CPUFlags.CARRY)));

        //set carry flag if bit 9 is 1
        setFlag(CPUFlags.CARRY, temp > 0b1111_1111);

        //set zero flag if low byte of temp is 0
        // temp & const = ?
        // 0 & 1 = 0
        // 1 & 1 = 1
        // so if low byte all 0, & 0x00ff all zero
        setFlag(CPUFlags.ZERO, (temp & 0b0000_0000_1111_1111) == 0);

        //set signed overflow flag based on logic above
        //num is negative is Byte toUnsignedInt > 127
        //if pos + neg
        setFlag(CPUFlags.OVERFLOW, false);
        if(a > 0 && fetched > 0){
            if (fetched > (Byte.MAX_VALUE - a - getFlag(CPUFlags.CARRY))) setFlag(CPUFlags.OVERFLOW, true);
        }
        if(a < 0 && fetched < 0){
            if (fetched < (Byte.MIN_VALUE - a - getFlag(CPUFlags.CARRY))) setFlag(CPUFlags.OVERFLOW, true);
        }

        // The negative flag is set to the most significant bit of the resultant byte
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);

        // Load the result(temp) into the accumulator
        a = (byte) temp;

        // This instruction has the potential to require an additional clock cycle
        return 1;
    }

    // Instruction: Subtraction with Borrow In
    // Function: A = A - M - (1 - C)
    // Flags Out: C, V, N, Z
    private byte SBC(){
        //fetch the data
        fetch();

        //math (should not go into int range, right??????)
        temp = (short)(Byte.toUnsignedInt(a) - Byte.toUnsignedInt(fetched) - (1-Byte.toUnsignedInt(getFlag(CPUFlags.CARRY))));

        //set carry flag if there needed to be a carry in order to do the operation
        // (the high byte of the short will be all ones)
        //e.g. 100-(-100) = 0b0110_0100 - 0b1001_1100
        //or 10 - 100 = 0b0000_1111 - 0b0110_0100
        setFlag(CPUFlags.CARRY, (temp & 0b11111111_00000000) == 0b11111111_00000000);

        //set the zero flag
        setFlag(CPUFlags.ZERO, (temp & 0b0000_0000_1111_1111) == 0);

        //set the overflow flag
        setFlag(CPUFlags.OVERFLOW, temp != a - fetched - (1 - getFlag(CPUFlags.CARRY)));

        // The negative flag is set to the most significant bit of the resultant byte
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);

        // load the result(low bytes of temp) into accumulator
        a = (byte) temp;

        // This instruction has the potential to require an additional clock cycle
        return 1;
    }

    // how to opcode
    // 1) Fetch the data
    // 2) Perform calculation
    // 3) Store the result in desired place
    // 4) Set Flags of the status register
    // 5) Return 1 if instruction has potential to require additional clock cycle

    // Instruction: Bitwise Logic AND
    // Function:    A = A & F
    // Flags Out:   N, Z
    private byte AND(){
        fetch();
        a = (byte)(a & fetched);
        setFlag(CPUFlags.ZERO, a == 0b0000_0000);
        setFlag(CPUFlags.NEGATIVE, (a & 0b1000_0000) == 0b1000_0000);
        return 1;
    }

    // Instruction: Arithmetic Shift Left
    // Function:    A = C <- (A << 1) <- 0
    // Flags Out:   N, Z, C
    private byte ASL(){
        fetch();

        temp = (short)(UnsignedMath.byteToShort(fetched) << 1);
        setFlag(CPUFlags.CARRY, (temp & 0b11111111_00000000) > 0); // if there are set bits in high byte, value >0
        setFlag(CPUFlags.ZERO, (temp & 0b0000_0000_1111_1111) == 0b0000_0000_0000_0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b0000_0000_1000_0000) == 0b0000_0000_1000_0000);

        return 0;
    }

    // Instruction: Branch if Carry Clear
    // Function:    if(C == 0) pc = address
    private byte BCC(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.CARRY) == 0)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Instruction: Branch if Carry Set
    // Function:    if(C == 1) pc = address
    // literally the op above but c = 1
    private byte BCS(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.CARRY) == 1)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Instruction: Branch if Result Zero
    // Function:    if(Z == 1) pc = address
    private byte BEQ(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.ZERO) == 1)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Test Bits in Memory with Accumulator
    // bits 7 and 6 of operand are transferred to bit 7 and 6 of SR (N,V);
    // the zero-flag is set to the result of operand AND accumulator.
    // A AND M, F7 -> N, F6 -> V
    private byte BIT(){
        fetch();
        temp = UnsignedMath.byteToShort((byte)(a & fetched));
        setFlag(CPUFlags.ZERO, (temp & 0b00000000_11111111) == 0);
        setFlag(CPUFlags.NEGATIVE, (fetched & 0b1000_0000) == 0b1000_0000);
        setFlag(CPUFlags.OVERFLOW, (fetched & 0b0100_0000) == 0b0100_0000);
        return 0;
    }

    // Instruction: Branch if Negative
    // Function:    if(N == 1) pc = address
    private byte BMI(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.NEGATIVE) == 1)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Instruction: Branch if Not Equal
    // Function:    if(Z == 0) pc = address
    private byte BNE(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.ZERO) == 0)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Instruction: Branch if Positive
    // Function:    if(N == 0) pc = address
    private byte BPL(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.NEGATIVE) == 0)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Force Break
    // BRK initiates a software interrupt similar to a hardware interrupt (IRQ).
    // The return address pushed to the stack is PC+2, providing an extra byte of spacing for a break mark
    // (identifying a reason for the break.)
    // The status register will be pushed to the stack with the break flag set to 1.
    // However, when retrieved during RTI or by a PLP instruction, the break flag will be ignored.
    // The interrupt disable flag is not set automatically.
    // interrupt, push PC+2, push StatR

    private byte BRK()
    {
        programCounter++;

        setFlag(CPUFlags.D_INTERRUPT, true);
        this.activelyWrite((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)), (byte)((programCounter >>> 8) & 0x00FF));
        stackPointer--;
        this.activelyWrite((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)), (byte)(programCounter & 0x00FF));
        stackPointer--;

        setFlag(CPUFlags.BREAK,true);
        this.activelyWrite((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)), stat_regs);
        stackPointer--;
        setFlag(CPUFlags.BREAK,false);

        programCounter = (short) (UnsignedMath.byteToShort(this.activelyRead((short) 0xFFFE))
                                    | (this.activelyRead((short) 0xFFFF) << 8));
        return 0;
    }

    // Instruction: Branch if Overflow Clear
    // Function:    if(V == 0) pc = address
    private byte BVC(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.OVERFLOW) == 0)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Instruction: Branch if Overflow Set
    // Function:    if(V == 1) pc = address
    private byte BVS(){
        //uses the relative addr mode so no fetch needed
        if (getFlag(CPUFlags.OVERFLOW) == 1)
        {
            cycles++; //needs extra cycle
            addr_abs = (short)(programCounter + addr_rel);

            if((addr_abs & 0b11111111_00000000) != (programCounter & 0b11111111_00000000)) cycles++;

            programCounter = addr_abs;
        }
        return 0;
    }

    // Instruction: Clear Carry Flag
    // Function:    C = 0
    private byte CLC() {
        setFlag(CPUFlags.CARRY,false);
        return 0;
    }

    // Instruction: Clear Decimal Flag
    // Function:    D = 0
    private byte CLD() {
        setFlag(CPUFlags.DECIMAL,false);
        return 0;
    }

    // Instruction: Clear Interrupt Flag / disable interrupts
    // Function:    I = 0
    private byte CLI() {
        setFlag(CPUFlags.D_INTERRUPT,false);
        return 0;
    }

    // Instruction: Clear Overflow Flag
    // Function:    V = 0
    private byte CLV() {
        setFlag(CPUFlags.OVERFLOW,false);
        return 0;
    }

    // Instruction: Compare Accumulator
    // Function:    C <- A >= F      Z <- (A - F) == 0
    // Flags Out:   N, C, Z
    private byte CMP() {
        fetch();
        temp = (short)(UnsignedMath.byteToShort(a) - UnsignedMath.byteToShort(fetched));
        setFlag(CPUFlags.CARRY, a >= fetched);
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);
        return 1;
    }

    // Instruction: Compare X
    // Function:    C <- X >= F      Z <- (X - F) == 0
    // Flags Out:   N, C, Z
    private byte CPX() {
        fetch();
        temp = (short)(UnsignedMath.byteToShort(x) - UnsignedMath.byteToShort(fetched));
        setFlag(CPUFlags.CARRY, x >= fetched);
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Compare Y
    // Function:    C <- Y >= F      Z <- (Y - F) == 0
    // Flags Out:   N, C, Z
    private byte CPY() {
        fetch();
        temp = (short)(UnsignedMath.byteToShort(y) - UnsignedMath.byteToShort(fetched));
        setFlag(CPUFlags.CARRY, y >= fetched);
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Decrement Value at Memory Location
    // Function:    F = F - 1
    // Flags Out:   N, Z
    private byte DEC() {
        fetch();
        temp = UnsignedMath.byteToShort((byte) (fetched-1));
        this.activelyWrite(addr_abs, (byte)temp);
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Decrement X
    // Function:    X = X - 1
    // Flags Out:   N, Z
    private byte DEX() {
        x--;
        setFlag(CPUFlags.ZERO, x == 0x00);
        setFlag(CPUFlags.NEGATIVE, (x & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Decrement Y
    // Function:    Y = Y - 1
    // Flags Out:   N, Z
    private byte DEY() {
        y--;
        setFlag(CPUFlags.ZERO, y == 0x00);
        setFlag(CPUFlags.NEGATIVE, (y & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Bitwise Logic XOR
    // Function:    A = A xor F
    // Flags Out:   N, Z
    private byte EOR(){
        fetch();
        a ^= fetched;
        setFlag(CPUFlags.ZERO, a == 0x00);
        setFlag(CPUFlags.NEGATIVE, (a & 0b1000_0000) == 0b1000_0000);
        return 1;
    }

    // Instruction: Increment Value at Memory Location
    // Function:    F = F + 1
    // Flags Out:   N, Z
    private byte INC() {
        fetch();
        temp = UnsignedMath.byteToShort((byte) (fetched+1));
        this.activelyWrite(addr_abs, (byte)temp);
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Increment X
    // Function:    X = X + 1
    // Flags Out:   N, Z
    private byte INX() {
        x++;
        setFlag(CPUFlags.ZERO, x == 0x00);
        setFlag(CPUFlags.NEGATIVE, (x & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Increment Y
    // Function:    Y = Y + 1
    // Flags Out:   N, Z
    private byte INY() {
        y++;
        setFlag(CPUFlags.ZERO, y == 0x00);
        setFlag(CPUFlags.NEGATIVE, (y & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Jump To Location
    // Function:    pc = address
    private byte JMP() {
        // no need to fetch data since data ahead is already fetched into addr_abd (IND) or in next 2 bytes (ABS)
        programCounter = addr_abs;
        return 0;
    }

    // Instruction: Jump To Sub-Routine
    // Function:    Push current pc to stack, pc = address
    private byte JSR(){
        programCounter --;

        this.activelyWrite((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)), (byte)((programCounter >>> 8) & 0x00FF));
        stackPointer--;
        this.activelyWrite((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)), (byte)(programCounter & 0x00FF));
        stackPointer--;

        programCounter = addr_abs;
        return 0;
    }

    // Instruction: Fetch Into The Accumulator
    // Function:    A = F
    // Flags Out:   N, Z
    private byte LDA(){
        fetch();
        a = fetched;
        setFlag(CPUFlags.ZERO, a == 0x00);
        setFlag(CPUFlags.NEGATIVE, (a & 0b1000_0000) == 0b1000_0000);
        return 1;
    }

    // Instruction: Fetch Into X
    // Function:    X = F
    // Flags Out:   N, Z
    private byte LDX(){
        fetch();
        x = fetched;
        setFlag(CPUFlags.ZERO, x == 0x00);
        setFlag(CPUFlags.NEGATIVE, (x & 0b1000_0000) == 0b1000_0000);
        return 1;
    }

    // Instruction: Fetch Into Y
    // Function:    Y = F
    // Flags Out:   N, Z
    private byte LDY(){
        fetch();
        y = fetched;
        setFlag(CPUFlags.ZERO, y == 0x00);
        setFlag(CPUFlags.NEGATIVE, (y & 0b1000_0000) == 0b1000_0000);
        return 1;
    }

    // Shift Fetched or A One Bit Right
    // Function: 0 -> [76543210] -> C
    // Flags Out:  C, N = 0, Z
    private byte LSR(){
        fetch();
        setFlag(CPUFlags.CARRY, (fetched & 0b1) == 0b1);
        temp = (short)(fetched >>> 1);
        setFlag(CPUFlags.ZERO, temp == 0x00);
        setFlag(CPUFlags.NEGATIVE, false);
        if(isIMP){
            a = (byte) temp;
        } else {
            this.activelyWrite(addr_abs,(byte)temp);
        }
        return 0;
    }

    // NOP
    // some "illegal" NOPs can take extra cycle
    private byte NOP(){
        switch (opcode){
            //"illegal" NOPs
            case 0x1C:
            case 0x3C:
            case 0x5C:
            case 0x7C:
            case (byte) 0xDC:
            case (byte) 0xFC:
                return 1;
        }
        return 0;
    }

    // Instruction: Bitwise Logic OR
    // Function:    A = A | F
    // Flags Out:   N, Z
    private byte ORA(){
        fetch();
        a |= fetched;
        setFlag(CPUFlags.ZERO, a == 0x00);
        setFlag(CPUFlags.NEGATIVE, (a & 0b1000_0000) == 0b1000_0000);
        return 1;
    }

    // Instruction: Push Accumulator to Stack
    // Function:    A -> stack
    private byte PHA(){
        this.activelyWrite((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)), a);
        stackPointer--;
        return 0;
    }

    // Instruction: Push Stat Reg to Stack
    // Function:    SR -> stack
    // break flag & unused set before push
    private byte PHP(){
        this.activelyWrite((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)),
                            (byte)(stat_regs | CPUFlags.BREAK.getPosition() | CPUFlags.UNUSED.getPosition()));
        setFlag(CPUFlags.BREAK,false);
        setFlag(CPUFlags.UNUSED, true);
        stackPointer--;
        return 0;
    }

    // Instruction: Pop Accumulator off Stack
    // Function:    A <- stack
    // Flags Out:   N, Z
    private byte PLA(){
        stackPointer++;
        a = this.activelyRead((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)));
        setFlag(CPUFlags.ZERO, a == 0x00);
        setFlag(CPUFlags.NEGATIVE, (a & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Pop Stat Reg off Stack
    // Function:    SR <- stack
    // break flag and bit 5 ignored.
    private byte PLP(){
        stackPointer++;
        byte origBreakFlag = getFlag(CPUFlags.BREAK);
        stat_regs = this.activelyRead((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)));
        setFlag(CPUFlags.UNUSED, true);
        setFlag(CPUFlags.BREAK , origBreakFlag != 0);
        return 0;
    }

    // Instruction: Rotate One Bit Left (Memory or Accumulator)
    // Function: C <- [76543210] <- C
    private byte ROL(){
        fetch();
        temp = (short) ((fetched << 1) | getFlag(CPUFlags.CARRY));
        setFlag(CPUFlags.CARRY, (temp & 0b1_0000_0000) == 0b1_0000_0000);
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);
        if (isIMP) {
            a = (byte) temp;
        } else {
            this.activelyWrite(addr_abs, (byte) temp);
        }
        return 0;
    }

    // Instruction: Rotate One Bit Right (Memory or Accumulator)
    // Function: C -> [76543210] -> C
    private byte ROR(){
        fetch();
        temp = (short) ((getFlag(CPUFlags.CARRY) << 7) | (fetched >>> 1));
        setFlag(CPUFlags.CARRY, (fetched & 0b1) == 0b1);
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(CPUFlags.NEGATIVE, (temp & 0b1000_0000) == 0b1000_0000);
        if (isIMP) {
            a = (byte) temp;
        } else {
            this.activelyWrite(addr_abs, (byte) temp);
        }
        return 0;
    }

    // Instruction : Return from interrupt
    // Function :
    // The status register is pulled with the break flag and bit 5 ignored.
    // Then PC is pulled from the stack.
    // pull SR, pull PC
    private byte RTI(){
        //pull SR
        byte origBreakFlag = getFlag(CPUFlags.BREAK);
        stackPointer++;
        stat_regs = this.activelyRead((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)));
        setFlag(CPUFlags.UNUSED, true);
        setFlag(CPUFlags.BREAK , origBreakFlag != 0);

        //pull PC
        stackPointer++;
        byte lo_pc = this.activelyRead((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)));
        stackPointer++;
        byte hi_pc = this.activelyRead((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)));
        programCounter = UnsignedMath.byteToShort(hi_pc,lo_pc);

        return 0;
    }

    // Instruction : Return from Subroutine
    // Function :
    // pull PC, PC+1 -> PC
    private byte RTS(){
        stackPointer++;
        byte lo_pc = this.activelyRead((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)));
        stackPointer++;
        byte hi_pc = this.activelyRead((short)(0x0100 + UnsignedMath.byteToShort(stackPointer)));
        programCounter = UnsignedMath.byteToShort(hi_pc,lo_pc);
        programCounter++;
        return 0;
    }

    // Instruction: Set Carry Flag
    // Function:    C = 1
    private byte SEC()
    {
        setFlag(CPUFlags.CARRY, true);
        return 0;
    }

    // Instruction: Set Decimal Flag
    // Function:    D = 1
    private byte SED()
    {
        setFlag(CPUFlags.DECIMAL, true);
        return 0;
    }

    // Instruction: Set Interrupt Flag/ ENable Interrupt
    // Function:    I = 1
    private byte SEI()
    {
        setFlag(CPUFlags.D_INTERRUPT, true);
        return 0;
    }

    // Instruction: Store Accumulator at Address
    // Function:    F = A
    private byte STA(){
        this.activelyWrite(addr_abs, a);
        return 0;
    }

    // Instruction: Store X at Address
    // Function:    F = X
    private byte STX(){
        this.activelyWrite(addr_abs, x);
        return 0;
    }

    // Instruction: Store Y at Address
    // Function:    F = Y
    private byte STY(){
        this.activelyWrite(addr_abs, y);
        return 0;
    }

    // Instruction: Transfer A to X
    // Function:    X = A
    // Flags Out:   N, Z
    private byte TAX(){
        x = a;
        setFlag(CPUFlags.ZERO, x == 0x00);
        setFlag(CPUFlags.NEGATIVE, (x & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Transfer A to y
    // Function:    Y = A
    // Flags Out:   N, Z
    private byte TAY(){
        y = a;
        setFlag(CPUFlags.ZERO, y == 0x00);
        setFlag(CPUFlags.NEGATIVE, (y & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Transfer StkP to x
    // Function:    X = SP
    // Flags Out:   N, Z
    private byte TSX(){
        x = stackPointer;
        setFlag(CPUFlags.ZERO, x == 0x00);
        setFlag(CPUFlags.NEGATIVE, (x & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Transfer X to A
    // Function:    A = X
    // Flags Out:   N, Z
    private byte TXA(){
        a = x;
        setFlag(CPUFlags.ZERO, a == 0x00);
        setFlag(CPUFlags.NEGATIVE, (a & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    // Instruction: Transfer X to StkP
    // Function:    SP = X
    private byte TXS(){
        stackPointer = x;
        return 0;
    }

    // Instruction: Transfer Y to A
    // Function:    A = Y
    // Flags Out:   N, Z
    private byte TYA(){
        a = y;
        setFlag(CPUFlags.ZERO, a == 0x00);
        setFlag(CPUFlags.NEGATIVE, (a & 0b1000_0000) == 0b1000_0000);
        return 0;
    }

    //Illegal Opcode function
    private byte XXX(){
        // TODO: 14/8/2022 Add error handling stuff here i guess
        return 0;
    }


}
