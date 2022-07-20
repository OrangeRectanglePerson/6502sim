package com.example.mcusim;

public class RAM extends Device{

    private byte[] storage;

    public RAM(short _startAddress, short _endAddress){
        this.setStartAddress(_startAddress);
        this.setEndAddress(_endAddress);
        //create storage of bytes ranging from address start to end addresses (size is end - start + 1 bytes)
        storage = new byte[Short.compareUnsigned(_endAddress,_startAddress)+1];
    }

    //writes data into the ram
    @Override
    public void readFromAddress(short receiveAdr, byte data) {
        storage[Short.compareUnsigned(this.getStartAddress(),receiveAdr)] = data;
    }

    //reads data from ram when requested
    @Override
    public byte requestedAddressWrite(short requestedAdr) {
        return storage[Short.compareUnsigned(this.getStartAddress(),requestedAdr)];
    }

    //ram cannot push data onto bus or forcefully read data from it
    //these methods should NOT be used
    //(trust in the developer)
    @Override
    public void readAtAddress(short requestAdr, byte data) {
    }
    @Override
    public void writeToAddress(short requestAdr, byte data) {
        super.writeToAddress(requestAdr, data);
    }

}
