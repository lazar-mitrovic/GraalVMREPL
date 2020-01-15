package repl;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(final Stage stage) throws IOException {
        final Parent root = FXMLLoader.load(Main.class.getResource("/main.fxml"));
        final Scene scene = new Scene(root);
        stage.setScene(scene);
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