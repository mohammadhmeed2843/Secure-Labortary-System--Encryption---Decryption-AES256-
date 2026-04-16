package javafxapplication7.ui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafxapplication7.models.Role;
import javafxapplication7.session.Session;

public class MainLayoutController {

    @FXML private StackPane contentArea;

    @FXML private HBox  breadcrumbBar;
    @FXML private Label breadcrumbLabel;
    @FXML private Button btnBack;

    @FXML private Button navDashboard;
    @FXML private Button navUpload;
    @FXML private Button navRecords;
    @FXML private Button navPatientFiles;
    @FXML private Label  navAdminSection;
    @FXML private Button navAdminUsers;
    @FXML private Button navAdminAudit;
    @FXML private Button navAdminRecovery;

    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblAvatarInitial;

    private static MainLayoutController instance;
    private Button activeNav;

    @FXML
    public void initialize() {
        instance = this;

        if (Session.isLoggedIn()) {
            String fullName = Session.getUser().getFullName();
            if (lblUserName != null) lblUserName.setText(fullName);
            if (lblUserRole != null) lblUserRole.setText(Session.getUser().getRole().getDisplayName());
            if (lblAvatarInitial != null && fullName != null && !fullName.isBlank())
                lblAvatarInitial.setText(String.valueOf(fullName.trim().charAt(0)).toUpperCase());

            Role role = Session.getUser().getRole();
            boolean isAdmin        = role == Role.ADMIN;
            boolean isReceptionist = role == Role.RECEPTIONIST;
            boolean isDoctor       = role == Role.DOCTOR;

            setVisible(navUpload,        isReceptionist || isAdmin);
            setVisible(navRecords,       isReceptionist || isAdmin);
            setVisible(navPatientFiles,  isDoctor || isAdmin);
            setVisible(navAdminSection,  isAdmin);
            setVisible(navAdminUsers,    isAdmin);
            setVisible(navAdminAudit,    isAdmin);
            setVisible(navAdminRecovery, isAdmin);
        }

        loadContent("HomePage.fxml", null);
        activate(navDashboard);
    }

    // ── Static navigation API ─────────────────────────────────────────────────

    public static void navigateToDashboard() {
        if (instance != null) {
            instance.loadContent("HomePage.fxml", null);
            instance.activate(instance.navDashboard);
        }
    }

    public static void navigateToUpload() {
        if (instance != null) {
            instance.loadContent("UploadTest.fxml", "New Test Record");
            instance.activate(instance.navUpload);
        }
    }

    public static void navigateToRecords() {
        if (instance != null) {
            instance.loadContent("RecordList.fxml", "All Records");
            instance.activate(instance.navRecords);
        }
    }

    public static void navigateToPatientFiles() {
        if (instance != null) {
            instance.loadContent("PatientFiles.fxml", "Patient Files");
            instance.activate(instance.navPatientFiles);
        }
    }

    public static void navigateToAdminUsers() {
        if (instance != null) {
            instance.loadContent("AdminUsersPanel.fxml", "User Accounts");
            instance.activate(instance.navAdminUsers);
        }
    }

    public static void navigateToAdminAudit() {
        if (instance != null) {
            instance.loadContent("AdminAuditPanel.fxml", "Activity Log");
            instance.activate(instance.navAdminAudit);
        }
    }

    public static void navigateToAdminRecovery() {
        if (instance != null) {
            instance.loadContent("AdminRecoveryPanel.fxml", "File Recovery");
            instance.activate(instance.navAdminRecovery);
        }
    }

    public static void navigateTo(String fxmlFileName) {
        if (instance != null) instance.loadContent(fxmlFileName, null);
    }

    // Legacy aliases
    public static void navigateToEncrypt()  { navigateToUpload();        }
    public static void navigateToDecrypt()  { navigateToPatientFiles();  }
    public static void navigateToExport()   { navigateToPatientFiles();  }

    // ── Sidebar handlers ──────────────────────────────────────────────────────

    @FXML private void showDashboard()     { navigateToDashboard();     }
    @FXML private void showUpload()        { navigateToUpload();        }
    @FXML private void showRecords()       { navigateToRecords();       }
    @FXML private void showPatientFiles()  { navigateToPatientFiles();  }
    @FXML private void showAdminUsers()    { navigateToAdminUsers();    }
    @FXML private void showAdminAudit()    { navigateToAdminAudit();    }
    @FXML private void showAdminRecovery() { navigateToAdminRecovery(); }
    @FXML private void handleBack()        { navigateToDashboard();     }

    @FXML private void handleSignOut() {
        try {
            Session.logout();
            instance = null;
            Parent root = FXMLLoader.load(
                getClass().getResource("/javafxapplication7/resources/fxml/RoleSelect.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void loadContent(String fxmlFileName, String pageTitle) {
        try {
            Region content = FXMLLoader.load(
                    getClass().getResource(
                        "/javafxapplication7/resources/fxml/" + fxmlFileName));
            content.prefWidthProperty().bind(contentArea.widthProperty());
            content.prefHeightProperty().bind(contentArea.heightProperty());

            if (!contentArea.getChildren().isEmpty()) {
                Region old = (Region) contentArea.getChildren().get(0);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(90), old);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(ev -> {
                    contentArea.getChildren().setAll(content);
                    content.setOpacity(0);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), content);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                contentArea.getChildren().setAll(content);
                content.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), content);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }

            boolean onDashboard = (pageTitle == null);
            setVisible(breadcrumbBar, !onDashboard);
            if (!onDashboard && breadcrumbLabel != null)
                breadcrumbLabel.setText(pageTitle);

        } catch (Exception e) {
            e.printStackTrace();
            // Show a visible error label so navigation failures are never silent
            javafx.scene.control.Label errLabel = new javafx.scene.control.Label(
                "Failed to load page: " + fxmlFileName + "\n" + e.getMessage());
            errLabel.setStyle("-fx-text-fill:#C41230; -fx-font-size:13px; -fx-padding:40;");
            errLabel.setWrapText(true);
            contentArea.getChildren().setAll(errLabel);
        }
    }

    private void activate(Button nav) {
        if (nav == null) return;
        if (activeNav != null) activeNav.getStyleClass().remove("nav-item-active");
        if (!nav.getStyleClass().contains("nav-item-active"))
            nav.getStyleClass().add("nav-item-active");
        activeNav = nav;
    }

    private static void setVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }
}
