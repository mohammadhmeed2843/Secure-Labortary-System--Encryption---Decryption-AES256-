package javafxapplication7;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafxapplication7.model.Role;
import javafxapplication7.model.User;
import javafxapplication7.service.AdminService;
import javafxapplication7.service.AuditService;
import javafxapplication7.service.PermissionService;
import javafxapplication7.session.Session;

import java.util.List;

/** Admin-only screen: create users, activate/deactivate, reset passwords. */
public class AdminUsersController {

    // ── Create section ───────────────────────────────────────────────────────
    @FXML private TextField     newUsernameField;
    @FXML private TextField     newFullNameField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<String> newRoleBox;
    @FXML private Label         createStatusLabel;

    // ── User list ─────────────────────────────────────────────────────────────
    @FXML private VBox          userListBox;

    // ── Reset password section ───────────────────────────────────────────────
    @FXML private VBox          resetSection;
    @FXML private Label         resetTargetLabel;
    @FXML private PasswordField resetPasswordField;
    @FXML private Label         resetStatusLabel;

    private int resetTargetUserId = -1;

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn() ||
                !PermissionService.canManageUsers(Session.getUser().getRole())) {
            showCreateStatus("Access denied.", true);
            return;
        }
        newRoleBox.getItems().addAll("RECEPTIONIST", "DOCTOR", "ADMIN");
        loadUsers();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @FXML
    private void handleCreateUser() {
        String username = newUsernameField.getText().trim();
        String fullName = newFullNameField.getText().trim();
        String password = newPasswordField.getText();
        String roleStr  = newRoleBox.getValue();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || roleStr == null) {
            showCreateStatus("All fields are required.", true); return;
        }
        if (password.length() < 8) {
            showCreateStatus("Password must be at least 8 characters.", true); return;
        }

        Role role;
        try { role = Role.valueOf(roleStr); }
        catch (IllegalArgumentException e) {
            showCreateStatus("Invalid role selected.", true); return;
        }

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AdminService.createUser(username, password, role, fullName);
                AuditService.logCurrent(AuditService.CREATE_USER, "user",
                        username, "role=" + roleStr);
                return null;
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            showCreateStatus("User '" + username + "' created.", false);
            newUsernameField.clear();
            newFullNameField.clear();
            newPasswordField.clear();
            newRoleBox.setValue(null);
            loadUsers();
        }));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() ->
            showCreateStatus("Failed: " + task.getException().getMessage(), true)));

        new Thread(task, "smls-create-user").start();
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() { loadUsers(); }

    private void loadUsers() {
        Task<List<User>> task = new Task<>() {
            @Override protected List<User> call() throws Exception {
                return AdminService.listAllUsers();
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() ->
            buildUserList(task.getValue())));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() ->
            userListBox.getChildren().setAll(
                new Label("Failed to load users: " + task.getException().getMessage()))));

        new Thread(task, "smls-list-users").start();
    }

    private void buildUserList(List<User> users) {
        userListBox.getChildren().clear();
        for (User u : users) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding:10 14 10 14; -fx-background-radius:6;" +
                         (u.isActive()
                            ? "-fx-background-color:#151b27;"
                            : "-fx-background-color:#1a1a1a; -fx-opacity:0.7;"));

            Label nameLabel = new Label(u.getFullName());
            nameLabel.setStyle("-fx-text-fill:#e8eaf0; -fx-font-size:13px; -fx-font-weight:bold;");

            Label roleLabel = new Label(u.getRole().getDisplayName());
            roleLabel.setStyle("-fx-text-fill:#3b82f6; -fx-font-size:11px; " +
                               "-fx-background-color:#0d1b2e; -fx-padding:2 8 2 8; -fx-background-radius:10;");

            Label usernameLabel = new Label("@" + u.getUsername());
            usernameLabel.setStyle("-fx-text-fill:#4b5563; -fx-font-size:11px;");
            HBox.setHgrow(usernameLabel, Priority.ALWAYS);

            Button toggleBtn = new Button(u.isActive() ? "Deactivate" : "Activate");
            toggleBtn.setStyle(u.isActive()
                ? "-fx-background-color:#7f1d1d; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:4 10;"
                : "-fx-background-color:#14532d; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:4 10;");
            final int uid = u.getUserId();
            final boolean active = u.isActive();
            toggleBtn.setOnAction(e -> handleToggleActive(uid, active));

            // Don't allow admin to deactivate themselves
            if (uid == Session.getUser().getUserId()) toggleBtn.setDisable(true);

            Button resetBtn = new Button("Reset PW");
            resetBtn.setStyle("-fx-background-color:#1e293b; -fx-text-fill:#94a3b8; -fx-font-size:11px; -fx-padding:4 10;");
            resetBtn.setOnAction(e -> showResetSection(uid, u.getFullName()));

            row.getChildren().addAll(nameLabel, roleLabel, usernameLabel, toggleBtn, resetBtn);
            userListBox.getChildren().add(row);
        }
        if (users.isEmpty()) {
            userListBox.getChildren().add(new Label("No users found."));
        }
    }

    // ── Toggle active ─────────────────────────────────────────────────────────

    private void handleToggleActive(int userId, boolean currentlyActive) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AdminService.setUserActive(userId, !currentlyActive);
                String action = currentlyActive
                        ? AuditService.DEACTIVATE_USER : AuditService.ACTIVATE_USER;
                AuditService.logCurrent(action, "user", String.valueOf(userId), null);
                return null;
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(this::loadUsers));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() ->
            showCreateStatus("Toggle failed: " + task.getException().getMessage(), true)));

        new Thread(task, "smls-toggle-user").start();
    }

    // ── Reset password ────────────────────────────────────────────────────────

    private void showResetSection(int userId, String displayName) {
        resetTargetUserId = userId;
        resetTargetLabel.setText("Resetting password for: " + displayName);
        resetPasswordField.clear();
        resetStatusLabel.setText("");
        resetSection.setVisible(true);
        resetSection.setManaged(true);
    }

    @FXML
    private void handleResetPassword() {
        if (resetTargetUserId < 0) return;
        String pw = resetPasswordField.getText();
        if (pw.length() < 8) {
            resetStatusLabel.setText("Password must be at least 8 characters.");
            resetStatusLabel.setStyle("-fx-text-fill:#ef4444;");
            return;
        }
        final int uid = resetTargetUserId;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AdminService.resetPassword(uid, pw);
                AuditService.logCurrent(AuditService.RESET_PASSWORD, "user",
                        String.valueOf(uid), null);
                return null;
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            resetStatusLabel.setText("Password reset successfully.");
            resetStatusLabel.setStyle("-fx-text-fill:#10b981;");
            resetPasswordField.clear();
        }));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() -> {
            resetStatusLabel.setText("Failed: " + task.getException().getMessage());
            resetStatusLabel.setStyle("-fx-text-fill:#ef4444;");
        }));

        new Thread(task, "smls-reset-pw").start();
    }

    @FXML
    private void handleCancelReset() {
        resetSection.setVisible(false);
        resetSection.setManaged(false);
        resetTargetUserId = -1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showCreateStatus(String msg, boolean isError) {
        if (createStatusLabel == null) return;
        createStatusLabel.setText(msg);
        createStatusLabel.setStyle(isError
                ? "-fx-text-fill:#ef4444; -fx-font-size:12px;"
                : "-fx-text-fill:#10b981; -fx-font-size:12px;");
    }
}
