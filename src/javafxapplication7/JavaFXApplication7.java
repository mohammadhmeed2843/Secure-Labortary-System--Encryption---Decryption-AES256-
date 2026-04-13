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
        AuthService.seedDefaultUsersIfEmpty();

        Parent root = FXMLLoader.load(getClass().getResource("RoleSelect.fxml"));
        primaryStage.setTitle("Secure Medical Lab System");
        primaryStage.setMinWidth(680);
        primaryStage.setMinHeight(500);
        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
