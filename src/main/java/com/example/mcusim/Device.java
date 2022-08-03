package com.example.mcusim;

public abstract class Device {

    // TODO: 20/7/2022 Put classes for Devices in its own package

    //default access (subclasses & package)
    short startAddress;
    short endAddress;


    //use when this device is the "master" pushing data onto the bus
    public void activelyWrite(short requestAdr, byte data){
        Bus.serveDataToAdr(requestAdr,data);
    }

    //use when this device is on recipient end of an active write
    public abstract void passivelyRead(short requestedAdr, byte data);


    //this device requests data from a "slave" device through the bus
    //this method returns the value read from that address
    public abstract byte activelyRead(short requestAdr);

    //use when this device in on the "giver end" of a requested read
    //it returns data at the requestedAdr
    //it will be used later on for debugger/memory map display
    //by default returns 0 byte
    public byte readFromAdr(short requestedAdr) { return 0x00; }



    //getters and setters
    public short getStartAddress() {
        return startAddress;
    }
    public void setStartAddress(short startAddress) {
        this.startAddress = startAddress;
    }
    public short getEndAddress() {
        return endAddress;
    }
    public void setEndAddress(short endAddress) {
        this.endAddress = endAddress;
    }
}
