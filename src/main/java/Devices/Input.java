package Devices;

import javafx.scene.input.KeyCode;

import java.util.ArrayList;

public class Input extends Device{


    private final byte[] inputStorage = new byte[2]; //stores 2 byte value of what key is pressed
    private boolean stickyKeys; // if true, inputStorage will not clear after key release. it will otherwise
    private ArrayList<Character> allowedCharacters = new ArrayList<>(); // storage for characters to detect, nullify if to detect all keys
    private boolean sendKeyPressInterrupts; //if true, send IRQs to the CPU upon keypress
    private boolean keyPressRegistered; // set true if current keypress has been registered. falsify on key release

    public Input(String _name, short _startAddress){
        this.deviceName = _name;
        this.setStartAddress(_startAddress);
        // because input size is set to 2 byte value corresponding to char pressed
        // end address is the address after startAddress
        this.setEndAddress((short) (_startAddress + 1));
    }

    //reads data from inputStorage when CPU reads input
    @Override
    public byte readFromAdr(short requestedAdr) {
        synchronized (this) { return inputStorage[Short.compareUnsigned(requestedAdr,this.getStartAddress())]; }
    }

    public void registerKeyPress(KeyCode kc){
        char key_pressed = kc.getChar().charAt(0);
        inputStorage[0] = (byte) key_pressed;
        inputStorage[1] = (byte) (key_pressed >>> 7);
    }

    public void clearKey(){
        //set key value to 0x0000
        inputStorage[0] = 0;
        inputStorage[1] = 0;
    }


    //input cannot push data onto bus or forcefully read data from it
    // you should not be able to write into input
    //these methods should NOT be used
    //(trust in the developer)
    @Override
    public byte activelyRead(short requestAdr) {return 0;}
    @Override
    public void activelyWrite(short requestAdr, byte data) {
        super.activelyWrite(requestAdr, data);
    }
    @Override
    public void passivelyRead(short receiveAdr, byte data) {
    }

    // modify address setter


    @Override
    public void setStartAddress(short startAddress) {
        super.setStartAddress(startAddress);
        super.setEndAddress((short)(startAddress+1));
    }

    // sticky keys getter settter
    public boolean isStickyKeys() {
        return stickyKeys;
    }

    public void setStickyKeys(boolean stickyKeys) {
        this.stickyKeys = stickyKeys;
    }

    public ArrayList<Character> getAllowedCharacters() {
        return allowedCharacters;
    }

    public void setAllowedCharacters(char[] allowedCharacters) {
        if(this.allowedCharacters == null) this.allowedCharacters = new ArrayList<>();
        this.allowedCharacters.clear();
        for (char c : allowedCharacters) {
            this.allowedCharacters.add(c);
        }
    }

    public void allAllowedCharacters(){
        this.allowedCharacters = null;
    }

    public boolean isSendKeyPressInterrupts() {
        return sendKeyPressInterrupts;
    }

    public void setSendKeyPressInterrupts(boolean sendKeyPressInterrupts) {
        this.sendKeyPressInterrupts = sendKeyPressInterrupts;
    }

    public boolean isKeyPressRegistered() {
        return keyPressRegistered;
    }

    public void setKeyPressRegistered(boolean keyPressRegistered) {
        this.keyPressRegistered = keyPressRegistered;
    }
}
