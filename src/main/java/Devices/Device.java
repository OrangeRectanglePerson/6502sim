package Devices;

import MainComComponents.Bus;

public abstract class Device {

    //default access (subclasses & package)
    short startAddress;
    short endAddress;

    //device name
    String deviceName;


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

    @Override
    public String toString() {
        String startHex = Integer.toHexString(Short.toUnsignedInt(this.startAddress));
        String endHex = Integer.toHexString(Short.toUnsignedInt(this.endAddress));
        return String.format("%s%n0x%4s -> 0x%4s",this.deviceName, startHex, endHex);
    }
}
