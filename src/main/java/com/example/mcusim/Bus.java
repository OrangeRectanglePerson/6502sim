package com.example.mcusim;

import java.util.ArrayList;

public class Bus {

    public static ArrayList<Device> devices;

    public static void serveDataToAdr(short serveToAdr, byte data){
        for(Device d : devices){
            if(Short.compareUnsigned(serveToAdr,d.startAddress) >= 0
                    && Short.compareUnsigned(serveToAdr,d.endAddress) <= 0) d.readAddress(serveToAdr,data);
        }
    }

}
