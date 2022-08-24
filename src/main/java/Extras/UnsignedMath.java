package Extras;

public class UnsignedMath {
    //Unsigned Math Class for unsigned stuff

    //whoops i apparently didnt need addByte and addShort
    //binary of signed a + signed b == binary of unsigned a + unsigned b
    public static byte addByte(byte a, byte b){ return (byte) (Byte.toUnsignedInt(a) + Byte.toUnsignedInt(b));}

    public static short addShort(short a, short b){ return (short) (Short.toUnsignedInt(a) + Short.toUnsignedInt(b));}

    //this part is still very useful though
    //this is because (short) byte_value will maintain sign bit
    public static short byteToShort(byte low_byte) {
        //default high byte to 0x00
        return (short) (Byte.toUnsignedInt(low_byte));
    }
    public static short byteToShort(byte high_byte, byte low_byte) {
        return (short) ((Byte.toUnsignedInt(high_byte) << 8) | Byte.toUnsignedInt(low_byte));
    }

}
