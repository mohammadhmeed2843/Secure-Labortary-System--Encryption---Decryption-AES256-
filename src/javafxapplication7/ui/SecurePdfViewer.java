package javafxapplication7.ui;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Secure in-app PDF viewer.
 *
 * Security features:
 *  - PDF bytes are NEVER written to disk; rendered entirely in memory.
 *  - Windows SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE) blocks screen
 *    capture, Print Screen, Snipping Tool, and OBS on Windows 10/11.
 *  - Every page has a semi-transparent watermark with the viewer's name and
 *    the exact timestamp — traceable if the screen is photographed.
 *  - No Save / Print buttons are exposed.
 */
public final class SecurePdfViewer {

    // ── Constants ────────────────────────────────────────────────────────────

    private static final float RENDER_DPI = 150f;   // quality / performance balance
    private static final int   PAGE_WIDTH = 840;    // display width in pixels

    // WDA_EXCLUDEFROMCAPTURE — blocks all screen capture APIs on Win10 2004+
    private static final WinDef.DWORD WDA_EXCLUDE = new WinDef.DWORD(0x00000011L);

    // ── JNA interface ────────────────────────────────────────────────────────

    /**
     * Minimal custom User32 binding that guarantees SetWindowDisplayAffinity
     * is present regardless of the JNA Platform version installed.
     */
    interface SecureUser32 extends StdCallLibrary {
        SecureUser32 INSTANCE = Native.load(
                "user32", SecureUser32.class, W32APIOptions.DEFAULT_OPTIONS);

        /** Finds a top-level window by its exact title. */
        WinDef.HWND FindWindowW(String lpClassName, String lpWindowName);

        /**
         * Sets screen-capture protection on a window.
         * dwAffinity = 0x00000011 → WDA_EXCLUDEFROMCAPTURE
         */
        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, WinDef.DWORD dwAffinity);
    }

    private SecurePdfViewer() {}

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Opens a new secure viewer window. Must be called from any thread;
     * UI work is dispatched to the JavaFX Application Thread automatically.
     *
     * @param pdfBytes    decrypted PDF content — never written to disk
     * @param patientName displayed in the header (may be null)
     * @param fileName    original file name (may be null)
     * @param viewerName  logged-in user's full name — burned into watermark
     */
    public static void open(byte[] pdfBytes,
                            String patientName,
                            String fileName,
                            String viewerName) {
        Platform.runLater(() -> showWindow(pdfBytes, patientName, fileName, viewerName));
    }

    // ── Window construction ──────────────────────────────────────────────────

    private static void showWindow(byte[] pdfBytes,
                                   String patientName,
                                   String fileName,
                                   String viewerName) {
        // Unique title — used by JNA to locate the HWND
        String winTitle = "Secure PDF — " + UUID.randomUUID();

        Stage stage = new Stage();
        stage.setTitle(winTitle);
        stage.setMinWidth(720);
        stage.setMinHeight(520);

        // ── Loading pane ─────────────────────────────────────────────────────
        VBox loadBox = new VBox(18);
        loadBox.setAlignment(Pos.CENTER);
        loadBox.setPrefSize(900, 650);
        loadBox.setStyle("-fx-background-color:#111827;");

        ProgressIndicator pi = new ProgressIndicator(-1);
        pi.setPrefSize(52, 52);
        pi.setStyle("-fx-progress-color:#C41230;");

        Label loadLbl = new Label("Rendering document…");
        loadLbl.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:13px;");

        Label secLbl = new Label("🔒  Screen capture protection is active");
        secLbl.setStyle("-fx-text-fill:#374151; -fx-font-size:11px;");

        loadBox.getChildren().addAll(pi, loadLbl, secLbl);

        // ── Page container ───────────────────────────────────────────────────
        VBox pageBox = new VBox(18);
        pageBox.setAlignment(Pos.TOP_CENTER);
        pageBox.setPadding(new Insets(28, 28, 40, 28));
        pageBox.setStyle("-fx-background-color:#1A1A2E;");

        ScrollPane scroll = new ScrollPane(loadBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:#111827; -fx-border-width:0;" +
                        "-fx-background:#111827;");

        // ── Root layout ──────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(buildHeader(patientName, fileName, stage));
        root.setCenter(scroll);
        root.setStyle("-fx-background-color:#111827;");

        stage.setScene(new Scene(root, 960, 840));
        stage.show();

        // ── Block screen capture immediately after show() ────────────────────
        scheduleScreenCaptureBlock(winTitle);

        // ── Render pages in a background thread ──────────────────────────────
        String watermark = (viewerName != null ? viewerName : "CONFIDENTIAL") + "  •  " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm"));

        Task<List<javafx.scene.image.Image>> task = new Task<>() {
            @Override
            protected List<javafx.scene.image.Image> call() throws Exception {
                return renderPages(pdfBytes, watermark);
            }
        };

        task.setOnSucceeded(ev -> {
            List<javafx.scene.image.Image> pages = task.getValue();
            pageBox.getChildren().clear();

            for (javafx.scene.image.Image img : pages) {
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.setFitWidth(PAGE_WIDTH);
                iv.setSmooth(true);
                iv.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),20,0,0,8);");
                pageBox.getChildren().add(iv);
            }

            Label footer = new Label(
                    "🔒  CONFIDENTIAL — " + watermark +
                    "  |  " + pages.size() + " page" + (pages.size() == 1 ? "" : "s"));
            footer.setStyle("-fx-text-fill:#374151; -fx-font-size:10px; -fx-padding:12 0 0 0;");
            pageBox.getChildren().add(footer);

            scroll.setContent(pageBox);

            // Re-apply after content swap — some Windows builds reset the affinity
            scheduleScreenCaptureBlock(winTitle);
        });

        task.setOnFailed(ev -> {
            loadBox.getChildren().clear();
            Label err = new Label(
                    "Could not render document:\n" +
                    task.getException().getMessage());
            err.setStyle("-fx-text-fill:#F87171; -fx-font-size:13px; -fx-wrap-text:true;");
            err.setWrapText(true);
            err.setMaxWidth(500);
            loadBox.getChildren().add(err);
        });

        Thread t = new Thread(task, "smls-pdf-render");
        t.setDaemon(true);
        t.start();
    }

    // ── Header bar ───────────────────────────────────────────────────────────

    private static HBox buildHeader(String patientName, String fileName, Stage stage) {
        HBox hb = new HBox(14);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setPadding(new Insets(14, 20, 14, 20));
        hb.setStyle("-fx-background-color:#0D1117;" +
                    "-fx-border-color:#1F2937 transparent transparent transparent;" +
                    "-fx-border-width:0 0 1 0;");

        Label lock = new Label("🔒");
        lock.setStyle("-fx-font-size:18px;");

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label title = new Label(patientName != null && !patientName.isBlank()
                ? patientName : "Patient Record");
        title.setStyle("-fx-text-fill:#F9FAFB; -fx-font-size:14px; -fx-font-weight:bold;");

        Label sub = new Label(fileName != null && !fileName.isBlank()
                ? fileName : "Lab Report");
        sub.setStyle("-fx-text-fill:#6B7280; -fx-font-size:11px;");

        titleBox.getChildren().addAll(title, sub);

        Label badge = new Label("SECURE  VIEW  ONLY");
        badge.setStyle("-fx-background-color:#1F2937; -fx-text-fill:#C41230;" +
                       "-fx-font-size:10px; -fx-font-weight:bold;" +
                       "-fx-padding:4 12; -fx-background-radius:20;");

        Button closeBtn = new Button("✕  Close");
        closeBtn.setStyle("-fx-background-color:#C41230; -fx-text-fill:white;" +
                          "-fx-font-size:12px; -fx-font-weight:bold;" +
                          "-fx-background-radius:8; -fx-padding:8 18; -fx-cursor:hand;" +
                          "-fx-border-width:0;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color:#A30E27; -fx-text-fill:white;" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-background-radius:8; -fx-padding:8 18; -fx-cursor:hand;" +
                "-fx-border-width:0;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color:#C41230; -fx-text-fill:white;" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-background-radius:8; -fx-padding:8 18; -fx-cursor:hand;" +
                "-fx-border-width:0;"));
        closeBtn.setOnAction(e -> stage.close());

        hb.getChildren().addAll(lock, titleBox, badge, closeBtn);
        return hb;
    }

    // ── Screen capture block ─────────────────────────────────────────────────

    /**
     * Runs on a short-lived daemon thread so it doesn't block the FX thread.
     * Sleeps 350ms to give Windows time to register the new HWND before lookup.
     */
    private static void scheduleScreenCaptureBlock(String windowTitle) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(350);
                WinDef.HWND hwnd = SecureUser32.INSTANCE.FindWindowW(null, windowTitle);
                if (hwnd != null) {
                    boolean ok = SecureUser32.INSTANCE
                            .SetWindowDisplayAffinity(hwnd, WDA_EXCLUDE);
                    System.out.println("[SecurePdfViewer] Screen capture block applied: " + ok);
                } else {
                    System.err.println("[SecurePdfViewer] HWND not found — title: " + windowTitle);
                }
            } catch (Throwable e) {
                // Non-fatal: viewer still works, just without capture block
                System.err.println("[SecurePdfViewer] Capture block unavailable: " + e.getMessage());
            }
        }, "smls-capture-block");
        t.setDaemon(true);
        t.start();
    }

    // ── PDF rendering ────────────────────────────────────────────────────────

    /**
     * Renders every page of the PDF at RENDER_DPI, burns the watermark,
     * and converts to JavaFX Image. The byte array is never written to disk.
     */
    private static List<javafx.scene.image.Image> renderPages(byte[] pdfBytes,
                                                               String watermark)
            throws Exception {
        List<javafx.scene.image.Image> result = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int n = doc.getNumberOfPages();
            for (int i = 0; i < n; i++) {
                BufferedImage bi = renderer.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB);
                bi = burnWatermark(bi, watermark);
                result.add(SwingFXUtils.toFXImage(bi, null));
            }
        }
        return result;
    }

    /**
     * Burns a tiled, diagonal, semi-transparent watermark onto the page image.
     * Uses Java 2D — no library dependency.
     */
    private static BufferedImage burnWatermark(BufferedImage img, String text) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font font = new Font("Arial", Font.BOLD, 30);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);

        // 12% opacity — visible if photographed, not distracting while reading
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
        g.setColor(new Color(180, 0, 0)); // dark red

        // Rotate 35° around the page centre, then tile
        AffineTransform saved = g.getTransform();
        g.rotate(Math.toRadians(-35), img.getWidth() / 2.0, img.getHeight() / 2.0);

        int rowStep = 130;
        int colStep = tw + 70;
        for (int y = -img.getHeight(); y < img.getHeight() * 2; y += rowStep) {
            for (int x = -img.getWidth(); x < img.getWidth() * 2; x += colStep) {
                g.drawString(text, x, y);
            }
        }

        g.setTransform(saved);
        g.dispose();
        return img;
    }
}
