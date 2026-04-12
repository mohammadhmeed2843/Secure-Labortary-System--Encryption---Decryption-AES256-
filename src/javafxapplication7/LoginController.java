package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafxapplication7.model.User;
import javafxapplication7.service.AuthService;
import javafxapplication7.session.Session;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        User user = AuthService.login(username, password);
        if (user == null) {
            showError("Invalid username or password.");
            passwordField.clear();
            usernameField.requestFocus();
            return;
        }

        Session.login(user);

        try {
            Parent shell = FXMLLoader.load(
                    getClass().getResource("/javafxapplication7/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(shell));
            stage.setMinWidth(880);
            stage.setMinHeight(580);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load the application. Please restart.");
        }
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Login Failed");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
