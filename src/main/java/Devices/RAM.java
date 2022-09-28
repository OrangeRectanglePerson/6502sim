package Devices;

public class RAM extends Device{

    private byte[] storage;

    public RAM(String _name, short _startAddress, short _endAddress){
        this.deviceName = _name;
        this.setStartAddress(_startAddress);
        this.setEndAddress(_endAddress);
        //create storage of bytes ranging from address start to end addresses (size is end - start + 1 bytes)
        storage = new byte[Short.compareUnsigned(_endAddress,_startAddress)+1];
    }

    //writes data into the ram
    @Override
    public void passivelyRead(short receiveAdr, byte data) {
        synchronized (this) { storage[Short.compareUnsigned(receiveAdr,this.getStartAddress())] = data; }
    }

    //reads data from ram when requested
    @Override
    public byte readFromAdr(short requestedAdr) {
        synchronized (this) { return storage[Short.compareUnsigned(requestedAdr,this.getStartAddress())]; }
    }

    //reset ram data
    public void resetRAM(){
        storage = new byte[storage.length];
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

    @Override
    public void setStartAddress(short _startAddress) {
        super.setStartAddress(_startAddress);
        if(Short.compareUnsigned(endAddress,startAddress)+1 > 0) this.storage = new byte[Short.compareUnsigned(endAddress,startAddress)+1];
        else this.storage = null;
    }

    @Override
    public void setEndAddress(short _endAddress) {
        super.setEndAddress(_endAddress);
        if(Short.compareUnsigned(endAddress,startAddress)+1 > 0) this.storage = new byte[Short.compareUnsigned(endAddress,startAddress)+1];
        else this.storage = null;
    }
}
