package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin(ActionEvent event) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        if ("admin".equals(user) && "1234".equals(pass)) {
            try {
                Parent home = FXMLLoader.load(getClass().getResource("/javafxapplication7/HomePage.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(home));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to load the home screen. Please restart the application.");
            }
        } else {
            showError("Invalid username or password. Please try again.");
            passwordField.clear();
            usernameField.requestFocus();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
