module com.example.mcusim {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.mcusim to javafx.fxml;
    exports com.example.mcusim;
}