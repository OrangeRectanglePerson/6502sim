package com.example.mcusim;

import java.util.ArrayList;

public class Bus {

    public static ArrayList<Device> devices;

    //bus gives data to device at requested address
    public static void serveDataToAdr(short serveToAdr, byte data){
        for(Device d : devices){
            if(Short.compareUnsigned(serveToAdr,d.startAddress) >= 0
               && Short.compareUnsigned(serveToAdr,d.endAddress) <= 0) {
                d.readFromAddress(serveToAdr,data);
                return;
            }
        }
    }

    public static byte serveDataFromAdr(short serveFromAdr, byte data){
        for(Device d : devices){
            if(Short.compareUnsigned(serveFromAdr,d.startAddress) >= 0
               && Short.compareUnsigned(serveFromAdr,d.endAddress) <= 0){
                return d.requestedAddressWrite(serveFromAdr);
            }
        }
        return (short)0x0000;
    }

}
