package FrontEnd;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.Objects;

public class SplashScreen {

    static Scene splash;
    final private AnchorPane pane;
    final private ParallelTransition splashPT;

    public SplashScreen()
    {
        pane = new AnchorPane();

        splash = new Scene(pane);
        splashPT = new ParallelTransition();
    }

    public void show()
    {

        ImageView frontImage = new ImageView(
                new Image(Objects.requireNonNull(SplashScreen.class
                        .getResourceAsStream("/FrontEnd/MOS_6502_AD.jpg")))
        );
        frontImage.setFitWidth(600);
        frontImage.setFitHeight(400);
        frontImage.setX(0);
        frontImage.setY(0);

        VBox behindBox = new VBox();
        
        Label backgroundLabelTop = new Label("Welcome To");
        backgroundLabelTop.setPrefWidth(600);
        backgroundLabelTop.setAlignment(Pos.CENTER);
        backgroundLabelTop.setTextAlignment(TextAlignment.CENTER);
        backgroundLabelTop.setStyle("-fx-font-family: Monospaced; -fx-font-size: 32;");

        ImageView backgroundImage = new ImageView(
                new Image(Objects.requireNonNull(SplashScreen.class
                        .getResourceAsStream("/FrontEnd/MOS_6502.jpg")))
        );
        backgroundImage.setFitWidth(600);
        backgroundImage.setFitHeight(300);
        backgroundImage.setX(0);
        backgroundImage.setY(0);

        Label backgroundLabelBottom = new Label("The Virtual MOS 6502 Microcomputer");
        backgroundLabelBottom.setPrefWidth(600);
        backgroundLabelBottom.setAlignment(Pos.CENTER);
        backgroundLabelBottom.setTextAlignment(TextAlignment.CENTER);
        backgroundLabelBottom.setStyle("-fx-font-family: Monospaced; -fx-font-size: 28");


        behindBox.getChildren().addAll(backgroundLabelTop, backgroundImage, backgroundLabelBottom);
        behindBox.setAlignment(Pos.CENTER);
        behindBox.setStyle("-fx-background-color: WHITE");
        behindBox.setPrefHeight(400);


        FadeTransition imagFT = new FadeTransition(Duration.seconds(2),frontImage);
        imagFT.setFromValue(1.0f);
        imagFT.setToValue(0.0f);
        imagFT.setCycleCount(1);

        SequentialTransition imagST = new SequentialTransition();
        imagST.getChildren().addAll(
                new PauseTransition(Duration.seconds(5)),
                imagFT,
                new PauseTransition(Duration.seconds(3))
        );

        AnchorPane.setTopAnchor(behindBox,0.0);
        AnchorPane.setLeftAnchor(behindBox,0.0);

        AnchorPane.setTopAnchor(frontImage,0.0);
        AnchorPane.setLeftAnchor(frontImage,0.0);

        splashPT.getChildren().add(imagST);

        pane.getChildren().addAll(behindBox, frontImage);

        splashPT.setCycleCount(1);
        splashPT.play();

        pane.setOnMousePressed(eh -> {
            // skip cutscene on mousepress
            splashPT.jumpTo("end");
        });
    }

    public Scene getSplashScene()
    {
        return splash;
    }

    public ParallelTransition getTimeline()
    {
        return splashPT;
    }
}
