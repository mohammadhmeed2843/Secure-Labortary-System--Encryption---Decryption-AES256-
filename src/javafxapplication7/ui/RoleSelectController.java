package javafxapplication7.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafxapplication7.models.Role;
import javafxapplication7.models.User;
import javafxapplication7.services.AuditService;
import javafxapplication7.services.AuthService;
import javafxapplication7.session.Session;

/**
 * Entry screen — role cards first, then password prompt.
 * No username required; each role has exactly one active account.
 */
public class RoleSelectController {

    @FXML private VBox cardReceptionist;
    @FXML private VBox cardDoctor;
    @FXML private VBox cardAdmin;

    @FXML private VBox              passwordPanel;
    @FXML private Label             selectedRoleLabel;
    @FXML private PasswordField     passwordField;
    @FXML private Label             errorLabel;
    @FXML private Button            signInButton;
    @FXML private ProgressIndicator loadingIndicator;

    private Role selectedRole;

    @FXML
    private void handleRoleSelected(MouseEvent e) {
        Object src = e.getSource();
        if      (src == cardReceptionist) selectRole(Role.RECEPTIONIST, cardReceptionist);
        else if (src == cardDoctor)       selectRole(Role.DOCTOR,       cardDoctor);
        else if (src == cardAdmin)        selectRole(Role.ADMIN,        cardAdmin);
    }

    private void selectRole(Role role, VBox activeCard) {
        selectedRole = role;
        clearCardHighlight(cardReceptionist);
        clearCardHighlight(cardDoctor);
        clearCardHighlight(cardAdmin);
        activeCard.getStyleClass().add("role-card-active");
        selectedRoleLabel.setText("Signing in as  " + role.getDisplayName());
        errorLabel.setText("");
        passwordField.clear();
        passwordPanel.setVisible(true);
        passwordPanel.setManaged(true);
        Platform.runLater(() -> passwordField.requestFocus());
    }

    private void clearCardHighlight(VBox card) {
        card.getStyleClass().remove("role-card-active");
    }

    @FXML
    private void handleCancel() {
        clearCardHighlight(cardReceptionist);
        clearCardHighlight(cardDoctor);
        clearCardHighlight(cardAdmin);
        passwordPanel.setVisible(false);
        passwordPanel.setManaged(false);
        selectedRole = null;
        errorLabel.setText("");
    }

    @FXML
    private void handleSignIn() {
        if (selectedRole == null) return;
        String password = passwordField.getText();
        if (password.isEmpty()) { showError("Please enter your password."); return; }

        setLoading(true);

        Task<User> task = new Task<>() {
            @Override protected User call() throws Exception {
                return AuthService.loginByRole(selectedRole, password);
            }
        };

        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            User user = task.getValue();
            if (user == null) {
                setLoading(false);
                showError("Incorrect password. Please try again.");
                passwordField.clear();
                passwordField.requestFocus();
            } else {
                Session.login(user);
                AuditService.logCurrent(AuditService.LOGIN, "session", null,
                        "role=" + selectedRole.name());
                loadMainLayout();
            }
        }));

        task.setOnFailed(ev -> Platform.runLater(() -> {
            setLoading(false);
            showError("Login failed: " + task.getException().getMessage());
        }));

        Thread t = new Thread(task, "smls-login");
        t.setDaemon(true);
        t.start();
    }

    private void loadMainLayout() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/javafxapplication7/resources/fxml/MainLayout.fxml"));
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMinWidth(860);
            stage.setMinHeight(580);
            stage.centerOnScreen();
        } catch (Exception e) {
            setLoading(false);
            showError("Failed to load application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        signInButton.setDisable(loading);
        passwordField.setDisable(loading);
    }

    private void showError(String msg) { errorLabel.setText(msg); }
}
