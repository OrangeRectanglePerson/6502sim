package MainComComponents;

import Extras.UnsignedMath;

public class CPU{

    //internal registers
    private short programCounter = 0x0000;
    private byte a = 0x00;
    private byte x = 0x00;
    private byte y = 0x00;
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
    private byte RTC(){
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
