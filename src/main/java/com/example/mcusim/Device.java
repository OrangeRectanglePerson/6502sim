package com.example.mcusim;

public abstract class Device {

    // TODO: 20/7/2022 Put classes for Devices in its own package

    //default access (subclasses & package)
    short startAddress;
    short endAddress;


    //device puts data onto bus to requested Address (forceful)
    public void writeToAddress(short requestAdr, byte data){
        Bus.serveDataToAdr(requestAdr,data);
    }

    //asks device for data located at requested Address (passive)
    public abstract byte requestedAddressWrite(short requestedAdr);

    //device reads data from device on requested address on the bus (forceful)
    public abstract void readAtAddress(short requestAdr, byte data);

    //device receives data from device on the bus. requestedAdr is Address other device used to request this device (passive)
    public abstract void readFromAddress(short requestedAdr, byte data);

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
