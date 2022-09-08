package FrontEnd;

import Devices.Device;
import Devices.RAM;
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
        Bus.devices.add(new RAM("RAM00", (short) 0x0000, (short) 0x00FF));
        Bus.devices.add(new RAM("RAM01", (short) 0x0100, (short) 0x01FF));
        Bus.devices.add(new RAM("RAMFF", (short) 0xFF00, (short) 0xFFFF));

        //add processor
        Bus.processor = new CPU();

    }
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(FrontApp.class.getResource("Front.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setResizable(true);
        stage.setTitle("6502 microprocessor microcomputer v0.0");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}