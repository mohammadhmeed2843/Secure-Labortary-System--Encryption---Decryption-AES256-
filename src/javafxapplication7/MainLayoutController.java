package javafxapplication7;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Controls the persistent application shell: sidebar navigation + content area.
 *
 * <p>Content controllers call the static {@code navigateTo*()} methods to
 * swap the center panel without replacing the whole scene.</p>
 */
public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private Button navDashboard;
    @FXML private Button navEncrypt;
    @FXML private Button navDecrypt;

    private static MainLayoutController instance;
    private Button activeNav;

    @FXML
    public void initialize() {
        instance = this;
        loadContent("HomePage.fxml");
        activate(navDashboard);
    }

    // ── Static navigation API (called by content controllers) ────────────────

    /** Navigate to the Dashboard and highlight its sidebar item. */
    public static void navigateToDashboard() {
        if (instance != null) {
            instance.loadContent("HomePage.fxml");
            instance.activate(instance.navDashboard);
        }
    }

    /** Navigate to the Encrypt screen and highlight its sidebar item. */
    public static void navigateToEncrypt() {
        if (instance != null) {
            instance.loadContent("openEncryption.fxml");
            instance.activate(instance.navEncrypt);
        }
    }

    /** Navigate to the Decrypt screen and highlight its sidebar item. */
    public static void navigateToDecrypt() {
        if (instance != null) {
            instance.loadContent("openDecryption.fxml");
            instance.activate(instance.navDecrypt);
        }
    }

    /**
     * Navigate to any FXML file by name without changing the sidebar active state.
     * Use this for sub-step screens (e.g., PatientForm after encryption).
     */
    public static void navigateTo(String fxmlFileName) {
        if (instance != null) instance.loadContent(fxmlFileName);
    }

    // ── Sidebar button handlers ───────────────────────────────────────────────

    @FXML private void showDashboard() { navigateToDashboard(); }
    @FXML private void showEncrypt()   { navigateToEncrypt();   }
    @FXML private void showDecrypt()   { navigateToDecrypt();   }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private void loadContent(String fxmlFileName) {
        try {
            Region content = FXMLLoader.load(
                    getClass().getResource("/javafxapplication7/" + fxmlFileName));
            // Bind content size to fill the StackPane
            content.prefWidthProperty().bind(contentArea.widthProperty());
            content.prefHeightProperty().bind(contentArea.heightProperty());
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void activate(Button nav) {
        if (activeNav != null) activeNav.getStyleClass().remove("nav-item-active");
        if (!nav.getStyleClass().contains("nav-item-active"))
            nav.getStyleClass().add("nav-item-active");
        activeNav = nav;
    }
}
