package MainComComponents;

import Devices.Device;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class Bus {

    public static ObservableList<Device> devices = FXCollections.observableArrayList();

    public static CPU processor;

    //bus gives data to device at requested address
    public static void serveDataToAdr(short serveToAdr, byte data){
        for(Device d : devices){
            if(Short.compareUnsigned(serveToAdr,d.getStartAddress()) >= 0
               && Short.compareUnsigned(serveToAdr,d.getEndAddress()) <= 0) {
                d.passivelyRead(serveToAdr,data);
                return;
            }
        }
    }

    public static byte serveDataFromAdr(short serveFromAdr){
        for(Device d : devices){
            if(Short.compareUnsigned(serveFromAdr,d.getStartAddress()) >= 0
               && Short.compareUnsigned(serveFromAdr,d.getEndAddress()) <= 0){
                return d.readFromAdr(serveFromAdr);
            }
        }
        return 0x00;
    }

}
