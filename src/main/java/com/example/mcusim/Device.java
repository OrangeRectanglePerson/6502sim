package com.example.mcusim;

public abstract class Device {

    public short startAddress;
    public short endAddress;


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



}
