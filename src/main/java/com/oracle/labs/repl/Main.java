package com.oracle.labs.repl;

import com.gluonhq.attach.display.DisplayService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(final Stage stage) throws IOException {
        final Parent root = FXMLLoader.load(Main.class.getResource("/main.fxml"));
        Dimension2D dimension2D = DisplayService.create().map(d -> d.getDefaultDimensions()).orElse(new Dimension2D(667, 350));
        final Scene scene = new Scene(root, dimension2D.getWidth(), dimension2D.getHeight(), Color.web("#03687f"));
        stage.setScene(scene);
        stage.setTitle("GraalVM REPL");
        stage.show();

        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(final String[] args) {
        launch(args);
    }

}