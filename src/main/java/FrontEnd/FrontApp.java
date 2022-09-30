package FrontEnd;

import Devices.*;
import MainComComponents.Bus;
import MainComComponents.CPU;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

public class FrontApp extends Application {
    @Override
    public void init(){

        //add default devices
        //4K RAM
        Bus.devices.add(new RAM("RAM 00", (short) 0x0000, (short) 0x01FF));
        Bus.devices.add(new RAM("RAM 02", (short) 0x0200, (short) 0x03FF));
        Bus.devices.add(new RAM("RAM 04", (short) 0x0400, (short) 0x05FF));
        Bus.devices.add(new RAM("RAM 06", (short) 0x0600, (short) 0x07FF));
        Bus.devices.add(new RAM("RAM 08", (short) 0x0800, (short) 0x09FF));
        Bus.devices.add(new RAM("RAM 0A", (short) 0x0A00, (short) 0x0BFF));
        Bus.devices.add(new RAM("RAM 0C", (short) 0x0C00, (short) 0x0DFF));
        Bus.devices.add(new RAM("RAM 0E", (short) 0x0E00, (short) 0x0FFF));

        //16K ROM
        Bus.devices.add(new ROM("ROM", (short) 0x1000, (short) 0x4FFF));

        //display
        Bus.devices.add(new Display("Display",(short) 0x6000));

        //0xFFFB+ header
        Bus.devices.add(new ROM("Header ROM", (short) 0xFFFA, (short) 0xFFFF));


        //add processor
        Bus.processor = new CPU();

    }
    @Override
    public void start(Stage stage){
        SplashScreen ss = new SplashScreen();
        ss.show();
        stage.setScene(ss.getSplashScene());
        stage.getIcons().add(new Image(Objects.requireNonNull(FrontApp.class.getResourceAsStream("MOS_6502_Thumbnail.jpg"))));
        stage.setTitle("Starting Up");
        stage.setResizable(false);
        ss.getTimeline().setOnFinished(e -> {
            Timeline fadeoutTimeline = new Timeline();
            fadeoutTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.millis(500),
                            new KeyValue(ss.getSplashScene().getRoot().opacityProperty(), 0))
            );
            fadeoutTimeline.setOnFinished((event) -> {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(FrontApp.class.getResource("Front.fxml"));
                    Scene scene = new Scene(fxmlLoader.load());
                    stage.setResizable(true);
                    stage.setTitle("6502 microprocessor microcomputer Va.5");
                    stage.setScene(scene);
                    stage.setResizable(false);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            fadeoutTimeline.play();
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}