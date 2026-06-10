package javafxapplication7.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafxapplication7.models.Role;
import javafxapplication7.models.User;
import javafxapplication7.services.AuthService;

import java.util.Optional;

final class KioskExit {

    static void requestExit(Window owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Exit Kiosk");
        dialog.setHeaderText("Admin permission required");
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        ButtonType exitButton = new ButtonType("Exit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exitButton, ButtonType.CANCEL);

        PasswordField adminPassword = new PasswordField();
        adminPassword.setPromptText("Admin password");
        adminPassword.setMaxWidth(Double.MAX_VALUE);
        adminPassword.getStyleClass().add("form-field");
        dialog.getDialogPane().setContent(adminPassword);
        dialog.setResultConverter(button -> button == exitButton ? adminPassword.getText() : null);

        Platform.runLater(adminPassword::requestFocus);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        User admin = AuthService.login("admin", result.get());
        if (admin != null && admin.hasRole(Role.ADMIN)) {
            Platform.exit();
            System.exit(0);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exit Denied");
        alert.setHeaderText("Admin password required");
        alert.setContentText("The password you entered is not valid for the admin account.");
        alert.showAndWait();
    }

    private KioskExit() {}
}
