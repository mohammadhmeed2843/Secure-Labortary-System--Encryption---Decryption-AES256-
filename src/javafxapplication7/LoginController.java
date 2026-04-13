package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafxapplication7.model.User;
import javafxapplication7.service.AuthService;
import javafxapplication7.session.Session;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showInlineError("Username and password are required.");
            return;
        }

        User user = AuthService.login(username, password);
        if (user == null) {
            showInlineError("Invalid username or password.");
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
            stage.setMinWidth(920);
            stage.setMinHeight(600);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showInlineError("Failed to load application — please restart.");
        }
    }

    private void showInlineError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
    }
}
