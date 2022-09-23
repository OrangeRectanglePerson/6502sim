package FrontEnd;

import javafx.animation.*;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.Objects;

public class SplashScreen {

    static Scene splash;
    final private Pane pane;
    final private ParallelTransition splashPT;

    public SplashScreen()
    {
        pane = new Pane();

        splash = new Scene(pane);
        splashPT = new ParallelTransition();
    }

    public void show()
    {

        ImageView iv = new ImageView(
                new Image(Objects.requireNonNull(SplashScreen.class
                        .getResourceAsStream("/FrontEnd/MOS_6502_AD.jpg")))
        );
        iv.setFitWidth(600);
        iv.setFitHeight(400);
        iv.setX(0);
        iv.setY(0);

        splashPT.getChildren().add(new PauseTransition(Duration.seconds(5)));
        splashPT.setCycleCount(1);

        pane.getChildren().addAll(iv);

        splashPT.setCycleCount(1);
        splashPT.play();
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