package Devices;

public class RAM extends Device{

    @SuppressWarnings("FieldMayBeFinal")
    //idk why this warning suppression is needed because storage can be modified in passivelyRead
    private byte[] storage;

    public RAM(short _startAddress, short _endAddress){
        this.setStartAddress(_startAddress);
        this.setEndAddress(_endAddress);
        //create storage of bytes ranging from address start to end addresses (size is end - start + 1 bytes)
        storage = new byte[Short.compareUnsigned(_endAddress,_startAddress)+1];
    }

    //writes data into the ram
    @Override
    public void passivelyRead(short receiveAdr, byte data) {
        synchronized (this) { storage[Short.compareUnsigned(this.getStartAddress(),receiveAdr)] = data; }
    }

    //reads data from ram when requested
    @Override
    public byte readFromAdr(short requestedAdr) {
        synchronized (this) { return storage[Short.compareUnsigned(requestedAdr,this.getStartAddress())]; }
    }

    //ram cannot push data onto bus or forcefully read data from it
    //these methods should NOT be used
    //(trust in the developer)
    @Override
    public byte activelyRead(short requestAdr) {return 0;}
    @Override
    public void activelyWrite(short requestAdr, byte data) {
        super.activelyWrite(requestAdr, data);
    }

}
