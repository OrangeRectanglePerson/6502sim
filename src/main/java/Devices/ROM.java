package Devices;

public class ROM extends Device{

    @SuppressWarnings("FieldMayBeFinal")
    //idk why this warning suppression is needed because storage can be modified in passivelyRead
    private byte[] storage;

    public ROM(String _name, short _startAddress, short _endAddress){
        this.deviceName = _name;
        this.setStartAddress(_startAddress);
        this.setEndAddress(_endAddress);
        //create storage of bytes ranging from address start to end addresses (size is end - start + 1 bytes)
        storage = new byte[Short.compareUnsigned(_endAddress,_startAddress)+1];
    }


    //reads data from ROM when requested
    @Override
    public byte readFromAdr(short requestedAdr) {
        synchronized (this) { return storage[Short.compareUnsigned(requestedAdr,this.getStartAddress())]; }
    }

    //manually force in data (aka flashing the ROM)
    //for each byte in the byte array, if there is data to be flashed, flash
    //else, flash a 0
    public void flashROM(byte[] _inBytes){
        for (int i = 0; i < storage.length; i++) {
            try{
                storage[i] = _inBytes[i];
            } catch (IndexOutOfBoundsException ioobe){
                storage[i] = 0;
            }
        }
    }

    //return number of bytes ROM can store
    public int getROMSize(){
        return storage.length;
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
    //no writing data into the ROM
    @Override
    public void passivelyRead(short receiveAdr, byte data) {
    }


}
