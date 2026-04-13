package javafxapplication7;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafxapplication7.model.Role;
import javafxapplication7.session.Session;

public class MainLayoutController {

    @FXML private StackPane contentArea;

    // ── Sidebar nav buttons ───────────────────────────────────────────────────
    @FXML private Button navDashboard;
    @FXML private Button navUpload;          // RECEPTIONIST + ADMIN
    @FXML private Button navRecords;         // RECEPTIONIST + ADMIN
    @FXML private Button navPatientFiles;    // DOCTOR + ADMIN
    @FXML private Label  navAdminSection;    // ADMIN only (section header)
    @FXML private Button navAdminUsers;      // ADMIN only
    @FXML private Button navAdminAudit;      // ADMIN only
    @FXML private Button navAdminRecovery;   // ADMIN only

    // ── Footer labels ─────────────────────────────────────────────────────────
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    private static MainLayoutController instance;
    private Button activeNav;

    @FXML
    public void initialize() {
        instance = this;

        if (Session.isLoggedIn()) {
            if (lblUserName != null) lblUserName.setText(Session.getUser().getFullName());
            if (lblUserRole != null) lblUserRole.setText(Session.getUser().getRole().getDisplayName());

            Role role = Session.getUser().getRole();
            boolean isAdmin        = role == Role.ADMIN;
            boolean isReceptionist = role == Role.RECEPTIONIST;
            boolean isDoctor       = role == Role.DOCTOR;

            // New Record + All Records: RECEPTIONIST and ADMIN
            setVisible(navUpload,       isReceptionist || isAdmin);
            setVisible(navRecords,      isReceptionist || isAdmin);

            // Patient Files: DOCTOR and ADMIN
            setVisible(navPatientFiles, isDoctor || isAdmin);

            // Admin section: ADMIN only
            setVisible(navAdminSection, isAdmin);
            setVisible(navAdminUsers,   isAdmin);
            setVisible(navAdminAudit,   isAdmin);
            setVisible(navAdminRecovery,isAdmin);
        }

        loadContent("HomePage.fxml");
        activate(navDashboard);
    }

    // ── Static navigation API ─────────────────────────────────────────────────

    public static void navigateToDashboard() {
        if (instance != null) {
            instance.loadContent("HomePage.fxml");
            instance.activate(instance.navDashboard);
        }
    }

    public static void navigateToUpload() {
        if (instance != null) {
            instance.loadContent("UploadTest.fxml");
            instance.activate(instance.navUpload);
        }
    }

    public static void navigateToRecords() {
        if (instance != null) {
            instance.loadContent("RecordList.fxml");
            instance.activate(instance.navRecords);
        }
    }

    public static void navigateToPatientFiles() {
        if (instance != null) {
            instance.loadContent("PatientFiles.fxml");
            instance.activate(instance.navPatientFiles);
        }
    }

    public static void navigateToAdminUsers() {
        if (instance != null) {
            instance.loadContent("AdminUsersPanel.fxml");
            instance.activate(instance.navAdminUsers);
        }
    }

    public static void navigateToAdminAudit() {
        if (instance != null) {
            instance.loadContent("AdminAuditPanel.fxml");
            instance.activate(instance.navAdminAudit);
        }
    }

    public static void navigateToAdminRecovery() {
        if (instance != null) {
            instance.loadContent("AdminRecoveryPanel.fxml");
            instance.activate(instance.navAdminRecovery);
        }
    }

    /** Navigate to any FXML panel without changing active sidebar state. */
    public static void navigateTo(String fxmlFileName) {
        if (instance != null) instance.loadContent(fxmlFileName);
    }

    // Keep legacy names so any surviving references still compile
    public static void navigateToEncrypt() { navigateToUpload(); }
    public static void navigateToDecrypt() { navigateToPatientFiles(); }
    public static void navigateToExport()  { navigateToPatientFiles(); }

    // ── Sidebar button handlers ───────────────────────────────────────────────

    @FXML private void showDashboard()     { navigateToDashboard();     }
    @FXML private void showUpload()        { navigateToUpload();        }
    @FXML private void showRecords()       { navigateToRecords();       }
    @FXML private void showPatientFiles()  { navigateToPatientFiles();  }
    @FXML private void showAdminUsers()    { navigateToAdminUsers();    }
    @FXML private void showAdminAudit()    { navigateToAdminAudit();    }
    @FXML private void showAdminRecovery() { navigateToAdminRecovery(); }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void loadContent(String fxmlFileName) {
        try {
            Region content = FXMLLoader.load(
                    getClass().getResource("/javafxapplication7/" + fxmlFileName));
            content.prefWidthProperty().bind(contentArea.widthProperty());
            content.prefHeightProperty().bind(contentArea.heightProperty());
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
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
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
}
