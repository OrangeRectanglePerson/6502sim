package Devices;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class Display extends Device{

    //128x128 display size [16384 bytes/pixels]
    private byte[] BWVRAMstorage = new byte[2048]; //16384/8 = 2048
    private byte[] ColourVRAMstorage = new byte[16384];

    //store current VRAM in use (also use size of this to determine display mode)
    private byte[] currVRAM;

    public Display(String _name, short _startAddress){
        this.deviceName = _name;
        this.setStartAddress(_startAddress);
        //default display mode to BW
        currVRAM = BWVRAMstorage;
        this.setEndAddress((short) (_startAddress + (short) currVRAM.length - 1));
    }

    //writes data into the VRAM
    @Override
    public void passivelyRead(short receiveAdr, byte data) {
        synchronized (this) { currVRAM[Short.compareUnsigned(receiveAdr,this.getStartAddress())] = data; }
    }

    //reads data from VRAM when requested
    @Override
    public byte readFromAdr(short requestedAdr) {
        synchronized (this) { return currVRAM[Short.compareUnsigned(requestedAdr,this.getStartAddress())]; }
    }

    //reset VRAM data
    public void clearDisp(){
        BWVRAMstorage = new byte[2048];
        ColourVRAMstorage = new byte[16384];
    }

    //send out generated image based on VRAM content
    public Image getFrame(){
        WritableImage frame = new WritableImage(128,128);
        PixelWriter frameWriter = frame.getPixelWriter();
        byte[] framebuffer = new byte[16384*3];
;
        if(currVRAM.length == 2048){
            //System.out.println(2048);
            //BW image
            for(int i = 0; i < 16384; i++){
                //for every pixel...
                //default to black pixel
                framebuffer[3  *  i] = 0;
                framebuffer[3*i + 1] = 0;
                framebuffer[3*i + 2] = 0;
                //we use little endian :trolled:
                if((currVRAM[i/8] & ((byte)(0b1 << i%8))) != 0) {
                    framebuffer[3  *  i] = -1;
                    framebuffer[3*i + 1] = -1;
                    framebuffer[3*i + 2] = -1;
                }
            }
        } else {
            //System.out.println(16384);
            //Colour (6-bit RGB) image
            for(int i = 0; i < 16384; i++){
                //for every pixel...
                //default to black pixel
                framebuffer[3  *  i] = 0; //R
                framebuffer[3*i + 1] = 0; //G
                framebuffer[3*i + 2] = 0; //B
                //we use little endian :trolled: 0bXXBB_GGRR
                if((currVRAM[i] & ((byte)(0b0000_0011))) != 0) {
                    framebuffer[3  *  i] = (byte) ((currVRAM[i] & ((byte)(0b0000_0011))) * 85); //R
                }
                if((currVRAM[i] & ((byte)(0b0000_1100))) != 0) {
                    framebuffer[3  *  i] = (byte) (((currVRAM[i] & ((byte)(0b0000_1100)))>>>2) * 85); //G
                }
                if((currVRAM[i] & ((byte)(0b0011_0000))) != 0) {
                    framebuffer[3  *  i] = (byte) (((currVRAM[i] & ((byte)(0b0011_0000)))>>>4) * 85); //B
                }
            }
        }


        frameWriter.setPixels(0,0,128,128, PixelFormat.getByteRgbInstance(), framebuffer, 0, 128*3);

        return frame;
    }

    //Display cannot push data onto bus or forcefully read data from it
    //these methods should NOT be used
    //(trust in the developer)
    @Override
    public byte activelyRead(short requestAdr) {return 0;}
    @Override
    public void activelyWrite(short requestAdr, byte data) {
        super.activelyWrite(requestAdr, data);
    }
}
