package javafxapplication7;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafxapplication7.service.AuthService;

public class JavaFXApplication7 extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Seed the three default users on first launch if the table is empty
        AuthService.seedDefaultUsersIfEmpty();

        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
        primaryStage.setTitle("Secure Medical File System");
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(400);
        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
