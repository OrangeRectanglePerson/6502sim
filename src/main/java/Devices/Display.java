package Devices;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class Display extends Device{

    //store current VRAM in use (also use size of this to determine display mode)
    private byte[] currVRAM;

    public Display(String _name, short _startAddress){
        this.deviceName = _name;
        this.setStartAddress(_startAddress);
        //default display mode to BW
        this.setMode64BW();
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
        currVRAM = new byte[currVRAM.length];
    }

    //send out generated image based on VRAM content
    public Image getFrame(){
        byte[] framebuffer = new byte[0];

        if(currVRAM.length == 2048){
            //BW image 128x128
            framebuffer = new byte[16384*3];
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
        } else if (currVRAM.length == 16384){
            //Colour (6-bit RGB) image 128x128
            framebuffer = new byte[16384*3];
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
                    framebuffer[3*i + 1] = (byte) (((currVRAM[i] & ((byte)(0b0000_1100)))>>>2) * 85); //G
                }
                if((currVRAM[i] & ((byte)(0b0011_0000))) != 0) {
                    framebuffer[3*i + 2] = (byte) (((currVRAM[i] & ((byte)(0b0011_0000)))>>>4) * 85); //B
                }
            }
        } else if (currVRAM.length == 512){
            //BW image 64x64
            framebuffer = new byte[4096*3];
            for(int i = 0; i < 4096; i++){
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
        } else if (currVRAM.length == 4096){
            //6bit colour image 64x64
            framebuffer = new byte[4096*3];
            for(int i = 0; i < 4096; i++){
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
                    framebuffer[3*i + 1] = (byte) (((currVRAM[i] & ((byte)(0b0000_1100)))>>>2) * 85); //G
                }
                if((currVRAM[i] & ((byte)(0b0011_0000))) != 0) {
                    framebuffer[3*i + 2] = (byte) (((currVRAM[i] & ((byte)(0b0011_0000)))>>>4) * 85); //B
                }
            }
        }

        int dim = 0;

        if(framebuffer.length == 4096*3) dim = 64;
        if(framebuffer.length == 16384*3) dim = 128;

        WritableImage frame = new WritableImage(dim,dim);
        PixelWriter frameWriter = frame.getPixelWriter();
        frameWriter.setPixels(0, 0, dim, dim, PixelFormat.getByteRgbInstance(), framebuffer, 0, dim * 3);
        return frame;
    }

    //64x64 display size [4096 pixels]
    public void setMode64BW(){
        currVRAM = new byte[512];  //each bit is 1 pixel
        this.setEndAddress((short) (this.startAddress + (short) currVRAM.length - 1));
    }
    public void setMode64RGB(){
        currVRAM = new byte[4096]; //0xXXBB_GGRR
        this.setEndAddress((short) (this.startAddress + (short) currVRAM.length - 1));
    }
    //128x128 display size [16384 pixels]
    public void setMode128BW(){
        currVRAM = new byte[2048]; //each bit is 1 pixel
        this.setEndAddress((short) (this.startAddress + (short) currVRAM.length - 1));
    }
    public void setMode128RGB(){
        currVRAM = new byte[16384]; //0xXXBB_GGRR
        this.setEndAddress((short) (this.startAddress + (short) currVRAM.length - 1));
    }

    public int getVRAMSize(){
        return currVRAM.length;
        // 512   -> 64BW
        // 4096  -> 64RGB
        // 2048  -> 128BW
        // 16384 -> 128RGB
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
