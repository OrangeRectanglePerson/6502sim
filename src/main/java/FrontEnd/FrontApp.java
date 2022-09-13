package FrontEnd;

import Devices.Device;
import Devices.RAM;
import Devices.ROM;
import MainComComponents.Bus;
import MainComComponents.CPU;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class FrontApp extends Application {
    @Override
    public void init(){

        //add default devices
        Bus.devices.add(new RAM("RAM 00", (short) 0x0000, (short) 0x00FF));
        Bus.devices.add(new RAM("RAM 01", (short) 0x0100, (short) 0x01FF));
        Bus.devices.add(new ROM("ROM 02-0F", (short) 0x0200, (short) 0x0FFF));
        Bus.devices.add(new ROM("ROM FF", (short) 0xFF00, (short) 0xFFFF));

        //add processor
        Bus.processor = new CPU();

    }
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(FrontApp.class.getResource("Front.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setResizable(true);
        stage.setTitle("6502 microprocessor microcomputer Va.2");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}