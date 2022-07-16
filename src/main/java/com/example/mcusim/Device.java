package com.example.mcusim;

public abstract class Device {

    public short startAddress;
    public short endAddress;

    public void writeAddress(short writeToAdr, byte data){

    }

    public abstract void readAddress(short receiveAdr, byte data);

}
