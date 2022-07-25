package com.example.mcusim;

public class Tester {
    public static void main(String[] args) {
        Bus.devices.add(new RAM((short)0x0000,(short)0x00FF));

        String debug;

        debug = getDebugString((short) 0x0000, (short) 0x000f);
        System.out.println(debug);

        Bus.serveDataToAdr((short)0x0000, (byte) 0x0f);
        debug = getDebugString((short) 0x0000, (short) 0x000f);
        System.out.println(debug);
    }

    public static String getDebugString(short lowLimit, short highLimit){
        StringBuilder debug = new StringBuilder();
        for (int i=lowLimit; i < highLimit; i++){
            byte fetchvalue = Bus.serveDataFromAdr((short)i);
            debug.append(String.format("%s : %s%n", Integer.toHexString(i), Integer.toHexString(Byte.toUnsignedInt(fetchvalue))));
        }
        return debug.toString();
    }
}
