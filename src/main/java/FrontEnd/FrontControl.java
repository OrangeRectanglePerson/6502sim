package FrontEnd;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class FrontControl {
    @FXML
    private Label testLabel;

    @FXML
    protected void onMorbButtonClick() {
        testLabel.setText("Welcome to JavaFX Application!");
    }
}