package com.example.mcusim;

import java.util.ArrayList;

public class Bus {

    // TODO: 20/7/2022 Put Bus in a separate Package from the Devices

    public static ArrayList<Device> devices;

    //bus gives data to device at requested address
    public static void serveDataToAdr(short serveToAdr, byte data){
        for(Device d : devices){
            if(Short.compareUnsigned(serveToAdr,d.getStartAddress()) >= 0
               && Short.compareUnsigned(serveToAdr,d.getEndAddress()) <= 0) {
                d.readFromAddress(serveToAdr,data);
                return;
            }
        }
    }

    public static byte serveDataFromAdr(short serveFromAdr, byte data){
        for(Device d : devices){
            if(Short.compareUnsigned(serveFromAdr,d.getStartAddress()) >= 0
               && Short.compareUnsigned(serveFromAdr,d.getEndAddress()) <= 0){
                return d.requestedAddressWrite(serveFromAdr);
            }
        }
        return (short)0x0000;
    }

}
