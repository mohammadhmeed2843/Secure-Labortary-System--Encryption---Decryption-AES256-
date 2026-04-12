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
    @FXML private Button    navUpload;
    @FXML private Button    navExport;
    @FXML private Label     lblUserName;
    @FXML private Label     lblUserRole;

    private static MainLayoutController instance;
    private Button activeNav;

    @FXML
    public void initialize() {
        instance = this;

        // Show logged-in user info in sidebar footer
        if (Session.isLoggedIn()) {
            if (lblUserName != null) lblUserName.setText(Session.getUser().getFullName());
            if (lblUserRole != null) lblUserRole.setText(Session.getUser().getRole().getDisplayName());

            // Role-based nav visibility
            // DOCTOR  → cannot upload
            // TECHNICIAN → cannot export/decrypt
            boolean canUpload = Session.canUpload();
            boolean canExport = Session.canExport();
            if (navUpload != null) { navUpload.setVisible(canUpload); navUpload.setManaged(canUpload); }
            if (navExport != null) { navExport.setVisible(canExport); navExport.setManaged(canExport); }
        }

        loadContent("HomePage.fxml");
        activate(navDashboard);
    }

    // ── Static navigation API ─────────────────────────────────────────────────

    public static void navigateToDashboard() {
        if (instance != null) { instance.loadContent("HomePage.fxml"); instance.activate(instance.navDashboard); }
    }

    public static void navigateToUpload() {
        if (instance != null) { instance.loadContent("openEncryption.fxml"); instance.activate(instance.navUpload); }
    }

    public static void navigateToExport() {
        if (instance != null) { instance.loadContent("openDecryption.fxml"); instance.activate(instance.navExport); }
    }

    /** Navigate to any FXML panel without changing the active sidebar state. */
    public static void navigateTo(String fxmlFileName) {
        if (instance != null) instance.loadContent(fxmlFileName);
    }

    // Keep old names so any lingering references still compile
    public static void navigateToEncrypt() { navigateToUpload(); }
    public static void navigateToDecrypt() { navigateToExport(); }

    // ── Sidebar button handlers ───────────────────────────────────────────────

    @FXML private void showDashboard() { navigateToDashboard(); }
    @FXML private void showUpload()    { navigateToUpload();    }
    @FXML private void showExport()    { navigateToExport();    }

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
}
