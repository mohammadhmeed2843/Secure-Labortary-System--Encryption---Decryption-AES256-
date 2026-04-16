package javafxapplication7.ui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafxapplication7.models.FileRecord;
import javafxapplication7.services.AuditService;
import javafxapplication7.services.FileService;
import javafxapplication7.services.HistoryService;
import javafxapplication7.services.PermissionService;
import javafxapplication7.session.Session;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminRecoveryController {

    @FXML private VBox recoveryListBox;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn() ||
                !PermissionService.canArchive(Session.getUser().getRole())) {
            recoveryListBox.getChildren().add(new Label("Access denied."));
            return;
        }
        loadArchivedFiles();
    }

    @FXML private void handleRefresh() { loadArchivedFiles(); }

    private void loadArchivedFiles() {
        recoveryListBox.getChildren().setAll(new Label("Loading archived files…"));
        Task<List<FileRecord>> task = new Task<>() {
            @Override protected List<FileRecord> call() throws Exception {
                return HistoryService.listArchivedFiles();
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> buildList(task.getValue())));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() ->
            recoveryListBox.getChildren().setAll(
                new Label("Failed: " + task.getException().getMessage()))));

        Thread t = new Thread(task, "smls-recovery-load");
        t.setDaemon(true);
        t.start();
    }

    private void buildList(List<FileRecord> records) {
        recoveryListBox.getChildren().clear();
        if (records.isEmpty()) {
            recoveryListBox.getChildren().add(new Label("No archived records found."));
            return;
        }
        for (FileRecord r : records) {
            VBox card = new VBox(6);
            card.setStyle("-fx-padding:14 18 14 18; -fx-background-color:#FFFFFF; " +
                          "-fx-background-radius:10; -fx-border-color:#E5E7EB; " +
                          "-fx-border-radius:10; -fx-border-width:1; " +
                          "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

            HBox top = new HBox(12);
            top.setAlignment(Pos.CENTER_LEFT);

            Label patientName = new Label(r.getPatientName() != null
                    ? r.getPatientName() : r.getPatientNumber());
            patientName.setStyle("-fx-text-fill:#111827; -fx-font-size:13px; -fx-font-weight:bold;");

            Label testLabel = new Label(r.getTestType() != null ? r.getTestType() : "Unknown test");
            testLabel.setStyle("-fx-text-fill:#2563EB; -fx-font-size:11px; " +
                               "-fx-background-color:#DBEAFE; -fx-padding:2 8 2 8; -fx-background-radius:10;");

            Label versionLabel = new Label("v" + r.getFileVersion());
            versionLabel.setStyle("-fx-text-fill:#D97706; -fx-font-size:11px; " +
                                  "-fx-background-color:#FEF3C7; -fx-padding:2 8 2 8; -fx-background-radius:10;");

            Label archivedBadge = new Label("ARCHIVED");
            archivedBadge.setStyle("-fx-text-fill:#6B7280; -fx-font-size:10px; " +
                                   "-fx-background-color:#F3F4F6; -fx-padding:2 8 2 8; " +
                                   "-fx-background-radius:10; -fx-font-weight:bold;");

            Label dateLabel = new Label(r.getTestDate() != null ? r.getTestDate().format(FMT) : "—");
            dateLabel.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:11px;");
            HBox.setHgrow(dateLabel, Priority.ALWAYS);

            Button restoreBtn = new Button("↺ Restore");
            restoreBtn.setStyle("-fx-background-color:#C41230; -fx-text-fill:white; " +
                                "-fx-background-radius:7; -fx-font-size:12px; -fx-padding:6 16; -fx-cursor:hand;");
            restoreBtn.setOnAction(e -> handleRestore(r, restoreBtn));

            top.getChildren().addAll(patientName, testLabel, versionLabel, archivedBadge, dateLabel, restoreBtn);

            Label meta = new Label(
                "Record #" + r.getFileId() + "  ·  " +
                (r.getDoctorName() != null ? r.getDoctorName() : "—") + "  ·  " +
                (r.getOriginalName() != null ? r.getOriginalName() : "—") +
                (r.getPreviousVersionId() != null ? "  ·  previous version: #" + r.getPreviousVersionId() : ""));
            meta.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:11px;");

            card.getChildren().addAll(top, meta);
            recoveryListBox.getChildren().add(card);
        }
    }

    private void handleRestore(FileRecord archived, Button btn) {
        btn.setDisable(true);
        btn.setText("Restoring…");

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                List<FileRecord> allForPatient = FileService.listForPatient(archived.getPatientNumber());
                int currentFileId = -1;
                for (FileRecord r : allForPatient) {
                    if (!"ARCHIVED".equals(r.getStatus()) && r.getFileId() != archived.getFileId()) {
                        currentFileId = r.getFileId();
                        break;
                    }
                }
                if (currentFileId < 0) {
                    FileService.updateStatus(archived.getFileId(), "READY");
                } else {
                    HistoryService.restoreVersion(archived.getFileId(), currentFileId);
                }
                AuditService.logCurrent(AuditService.RESTORE_FILE, "file",
                        String.valueOf(archived.getFileId()),
                        "restored from archived; previous active=" + currentFileId);
                return null;
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText(null);
            a.setContentText("File #" + archived.getFileId() + " restored successfully.");
            a.showAndWait();
            loadArchivedFiles();
        }));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() -> {
            btn.setDisable(false);
            btn.setText("↺ Restore");
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Restore failed");
            a.setContentText(task.getException().getMessage());
            a.showAndWait();
        }));

        Thread t = new Thread(task, "smls-restore");
        t.setDaemon(true);
        t.start();
    }
}
