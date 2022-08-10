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
        fetched = a;
        return 0;
    }

    //Immediate
    //fetch the value after the instruction byte
    //load address of this value into addr_abs
    //then advance program counter to next address
    private byte IMM()
    {
        addr_abs = programCounter++;
        return 0;
    }

    //Zero Page
    //Access adress 0x0000 to 0x00FF (this means only 1 byte is needed to be fetched)
    //(this exists to save ROM space)
    private byte ZP0(){
        addr_abs = activelyRead(programCounter);
        programCounter++;
        addr_abs &= 0x00ff; // make sure high byte is empty
        return 0;
    }

    //Zero Page X Offset
    //Like Zero Page but the value in X is added to address read
    //(Accessible address range is still only 0x0000 to 0x00FF)
    private byte ZPX(){
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
        fetched = Bus.serveDataFromAdr(addr_abs);
    }

    //OPCODES

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
        setFlag(CPUFlags.CARRY, temp > 255);

        //set zero flag if low byte of temp is 0
        setFlag(CPUFlags.ZERO, (temp & 0x00FF) == 0);

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
        setFlag(CPUFlags.NEGATIVE, (temp & 0x0080) == 0x0080);

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
        setFlag(CPUFlags.CARRY, (temp & 0xff00) == 0xff00);

        //set the zero flag
        setFlag(CPUFlags.CARRY, (temp & 0x00ff) == 0);

        //set the overflow flag
        setFlag(CPUFlags.OVERFLOW, temp != a - fetched - (1 - getFlag(CPUFlags.CARRY)));

        // The negative flag is set to the most significant bit of the resultant byte
        setFlag(CPUFlags.NEGATIVE, (temp & 0x0080) == 0x0080);

        // load the result(low bytes of temp) into accumulator
        a = (byte) temp;

        // This instruction has the potential to require an additional clock cycle
        return 1;
    }



}
