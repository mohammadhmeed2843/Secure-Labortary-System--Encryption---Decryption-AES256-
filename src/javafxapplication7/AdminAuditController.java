package javafxapplication7;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafxapplication7.model.AuditEntry;
import javafxapplication7.service.AuditService;
import javafxapplication7.service.PermissionService;
import javafxapplication7.session.Session;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Admin-only screen: shows the audit_log table, newest first. */
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

    @FXML
    private void handleLoad() {
        Integer limit = limitBox.getValue();
        if (limit == null) limit = 100;
        loadLogs(limit);
    }

    @FXML
    private void handleRefresh() {
        Integer limit = limitBox.getValue();
        loadLogs(limit != null ? limit : 100);
    }

    private void loadLogs(int limit) {
        auditListBox.getChildren().setAll(new Label("Loading…"));
        final int lim = limit;
        Task<List<AuditEntry>> task = new Task<>() {
            @Override protected List<AuditEntry> call() throws Exception {
                return AuditService.getRecentLogs(lim);
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() ->
            buildList(task.getValue())));
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
            row.setStyle("-fx-padding:8 14 8 14; -fx-background-color:#151b27; -fx-background-radius:6;");

            Label time = new Label(e.getCreatedAt() != null
                    ? e.getCreatedAt().format(FMT) : "—");
            time.setStyle("-fx-text-fill:#4b5563; -fx-font-size:11px; -fx-min-width:160;");

            Label action = new Label(e.getAction());
            action.setStyle("-fx-text-fill:" + actionColor(e.getAction()) + "; " +
                            "-fx-font-size:11px; -fx-font-weight:bold; -fx-min-width:120;");

            Label user = new Label(e.getUsername() != null ? "@" + e.getUsername() : "—");
            user.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:11px; -fx-min-width:120;");

            Label target = new Label(
                (e.getTargetType() != null ? e.getTargetType() + " " : "") +
                (e.getTargetId()   != null ? "#" + e.getTargetId() : ""));
            target.setStyle("-fx-text-fill:#3b82f6; -fx-font-size:11px; -fx-min-width:100;");

            Label details = new Label(e.getDetails() != null ? e.getDetails() : "");
            details.setStyle("-fx-text-fill:#6b7280; -fx-font-size:11px;");
            HBox.setHgrow(details, Priority.ALWAYS);
            details.setWrapText(false);

            row.getChildren().addAll(time, action, user, target, details);
            auditListBox.getChildren().add(row);
        }
    }

    private static String actionColor(String action) {
        if (action == null) return "#9ca3af";
        return switch (action) {
            case AuditService.LOGIN          -> "#10b981";
            case AuditService.UPLOAD,
                 AuditService.UPDATE_FILE    -> "#3b82f6";
            case AuditService.EXPORT,
                 AuditService.VIEW           -> "#a855f7";
            case AuditService.DEACTIVATE_USER,
                 AuditService.ARCHIVE        -> "#ef4444";
            case AuditService.RESTORE_FILE,
                 AuditService.ACTIVATE_USER  -> "#f59e0b";
            case AuditService.RESET_PASSWORD,
                 AuditService.CREATE_USER    -> "#06b6d4";
            default                          -> "#9ca3af";
        };
    }
}
