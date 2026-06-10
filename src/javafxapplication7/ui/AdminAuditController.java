package javafxapplication7.ui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafxapplication7.models.AuditEntry;
import javafxapplication7.services.AuditService;
import javafxapplication7.services.PermissionService;
import javafxapplication7.session.Session;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminAuditController {

    @FXML private ComboBox<Integer> limitBox;
    @FXML private VBox              auditListBox;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn() ||
                !PermissionService.canManageUsers(Session.getUser().getRole())) {
            auditListBox.getChildren().add(new Label("Access denied."));
            return;
        }
        limitBox.getItems().addAll(50, 100, 250, 500);
        limitBox.setValue(100);
        loadLogs(100);
    }

    @FXML private void handleLoad()    { Integer l = limitBox.getValue(); loadLogs(l != null ? l : 100); }
    @FXML private void handleRefresh() { handleLoad(); }

    private void loadLogs(int limit) {
        auditListBox.getChildren().setAll(new Label("Loading…"));
        Task<List<AuditEntry>> task = new Task<>() {
            @Override protected List<AuditEntry> call() { return AuditService.getRecentLogs(limit); }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> buildList(task.getValue())));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() ->
            auditListBox.getChildren().setAll(
                new Label("Failed to load: " + task.getException().getMessage()))));

        Thread t = new Thread(task, "smls-audit-load");
        t.setDaemon(true);
        t.start();
    }

    private void buildList(List<AuditEntry> entries) {
        auditListBox.getChildren().clear();
        if (entries.isEmpty()) {
            auditListBox.getChildren().add(new Label("No audit entries found."));
            return;
        }
        for (AuditEntry e : entries) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding:9 14 9 14; -fx-background-color:#FFFFFF; " +
                         "-fx-background-radius:6; -fx-border-color:#E5E7EB; " +
                         "-fx-border-radius:6; -fx-border-width:1;");

            Label time = new Label(e.getCreatedAt() != null ? e.getCreatedAt().format(FMT) : "—");
            time.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:11px; -fx-min-width:160;");

            Label action = new Label(e.getAction());
            action.setStyle("-fx-text-fill:" + actionColor(e.getAction()) +
                            "; -fx-font-size:11px; -fx-font-weight:bold; -fx-min-width:130;");

            Label user = new Label(e.getUsername() != null ? "@" + e.getUsername() : "—");
            user.setStyle("-fx-text-fill:#6B7280; -fx-font-size:11px; -fx-min-width:120;");

            Label target = new Label(
                (e.getTargetType() != null ? e.getTargetType() + " " : "") +
                (e.getTargetId()   != null ? "#" + e.getTargetId() : ""));
            target.setStyle("-fx-text-fill:#C41230; -fx-font-size:11px; -fx-min-width:100;");

            Label details = new Label(e.getDetails() != null ? e.getDetails() : "");
            details.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:11px;");
            HBox.setHgrow(details, Priority.ALWAYS);
            details.setWrapText(false);

            row.getChildren().addAll(time, action, user, target, details);
            auditListBox.getChildren().add(row);
        }
    }

    private static String actionColor(String action) {
        if (action == null) return "#9CA3AF";
        return switch (action) {
            case AuditService.LOGIN          -> "#059669";
            case AuditService.UPLOAD,
                 AuditService.UPDATE_FILE    -> "#2563EB";
            case AuditService.EXPORT,
                 AuditService.VIEW           -> "#7C3AED";
            case AuditService.DEACTIVATE_USER,
                 AuditService.ARCHIVE        -> "#C41230";
            case AuditService.RESTORE_FILE,
                 AuditService.ACTIVATE_USER  -> "#D97706";
            case AuditService.RESET_PASSWORD,
                 AuditService.CREATE_USER    -> "#0284C7";
            default                          -> "#9CA3AF";
        };
    }
}
