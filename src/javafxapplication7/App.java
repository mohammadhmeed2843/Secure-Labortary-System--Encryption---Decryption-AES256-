package javafxapplication7;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafxapplication7.services.AuthService;

public class App extends Application {

    private static final boolean KIOSK_MODE
            = Boolean.parseBoolean(System.getProperty("kiosk", "true"));

    @Override
    public void start(Stage primaryStage) throws Exception {
        AuthService.seedDefaultUsersIfEmpty();

        Parent root = FXMLLoader.load(
                getClass().getResource("resources/fxml/RoleSelect.fxml"));
        primaryStage.setTitle("Secure Medical Lab System");
        if (KIOSK_MODE) {
            configureKioskMode(primaryStage);
        } else {
            primaryStage.setMinWidth(680);
            primaryStage.setMinHeight(500);
        }
        primaryStage.setScene(new Scene(root));
        if (!KIOSK_MODE) {
            primaryStage.centerOnScreen();
        }
        primaryStage.show();
        if (KIOSK_MODE) {
            enterKioskMode(primaryStage);
        }
    }

    private void configureKioskMode(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setOnCloseRequest(event -> event.consume());
        stage.sceneProperty().addListener((obs, oldScene, newScene)
                -> Platform.runLater(() -> enterKioskMode(stage)));
    }

    private void enterKioskMode(Stage stage) {
        stage.setMaximized(true);
        stage.setFullScreen(true);
        stage.toFront();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
