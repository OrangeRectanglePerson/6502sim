module com.example.mcusim {
    requires javafx.controls;
    requires javafx.fxml;


    opens Extras to javafx.fxml;
    exports Extras;
    exports Devices;
    opens Devices to javafx.fxml;
    exports MainComComponents;
    opens MainComComponents to javafx.fxml;
}