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
    @FXML private Button    navDashboard;
    @FXML private Button    navUpload;        // ADMIN + TECHNICIAN: "New Record"
    @FXML private Button    navRecords;       // ADMIN + TECHNICIAN: "All Records"
    @FXML private Button    navPatientFiles;  // DOCTOR: "Patient Files"
    @FXML private Label     lblUserName;
    @FXML private Label     lblUserRole;

    private static MainLayoutController instance;
    private Button activeNav;

    @FXML
    public void initialize() {
        instance = this;

        if (Session.isLoggedIn()) {
            if (lblUserName != null) lblUserName.setText(Session.getUser().getFullName());
            if (lblUserRole != null) lblUserRole.setText(Session.getUser().getRole().getDisplayName());

            boolean canUpload = Session.canUpload();   // ADMIN + TECHNICIAN
            boolean isDoctor  = Session.hasRole(Role.DOCTOR);

            // New Record — hidden for DOCTOR
            setVisible(navUpload, canUpload);
            // All Records — hidden for DOCTOR
            setVisible(navRecords, canUpload);
            // Patient Files — only for DOCTOR
            setVisible(navPatientFiles, isDoctor);
        }

        loadContent("HomePage.fxml");
        activate(navDashboard);
    }

    // ── Static navigation API ─────────────────────────────────────────────────

    public static void navigateToDashboard() {
        if (instance != null) { instance.loadContent("HomePage.fxml"); instance.activate(instance.navDashboard); }
    }

    /** Navigate to the unified upload screen (UploadTest.fxml). */
    public static void navigateToUpload() {
        if (instance != null) { instance.loadContent("UploadTest.fxml"); instance.activate(instance.navUpload); }
    }

    public static void navigateToRecords() {
        if (instance != null) { instance.loadContent("RecordList.fxml"); instance.activate(instance.navRecords); }
    }

    public static void navigateToPatientFiles() {
        if (instance != null) { instance.loadContent("PatientFiles.fxml"); instance.activate(instance.navPatientFiles); }
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

    @FXML private void showDashboard()    { navigateToDashboard();    }
    @FXML private void showUpload()       { navigateToUpload();       }
    @FXML private void showRecords()      { navigateToRecords();      }
    @FXML private void showPatientFiles() { navigateToPatientFiles(); }

    // ── Internal helpers ─────────────────────────────────────────────────────

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

    private static void setVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }
}
