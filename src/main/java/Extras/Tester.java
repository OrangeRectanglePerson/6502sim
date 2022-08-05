package Extras;

import Devices.RAM;
import MainComComponents.Bus;

public class Tester {
    public static void main(String[] args) {
        misc();
    }

    public static String getDebugString(short lowLimit, short highLimit){
        StringBuilder debug = new StringBuilder();
        for (int i=lowLimit; i < highLimit; i++){
            byte fetchvalue = Bus.serveDataFromAdr((short)i);
            debug.append(String.format("%s : %s%n", Integer.toHexString(i), Integer.toHexString(Byte.toUnsignedInt(fetchvalue))));
        }
        return debug.toString();
    }

    public static void testRam(){
        Bus.devices.add(new RAM((short)0x0000,(short)0x00FF));

        String debug;

        debug = getDebugString((short) 0x0000, (short) 0x000f);
        System.out.println(debug);

        Bus.serveDataToAdr((short)0x0000, (byte) 0x0f);
        debug = getDebugString((short) 0x0000, (short) 0x000f);
        System.out.println(debug);
    }

    public static void misc(){
        System.out.println("Start");
        for(byte i=-128; i < 127; i++){
            for(byte j=-128; j< 127; j++){
                if(UnsignedMath.addByte(i,j) != (byte)((0xffff&i)+(0xffff&j))){
                    System.out.println("\n" + i + " " + j);
                }
            }
        }
        System.out.println("end");
    }
}
