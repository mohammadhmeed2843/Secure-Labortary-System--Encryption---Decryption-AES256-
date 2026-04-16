package javafxapplication7.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafxapplication7.models.FileRecord;
import javafxapplication7.models.Role;
import javafxapplication7.services.FileService;
import javafxapplication7.services.PermissionService;
import javafxapplication7.session.Session;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RecordListController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private VBox             recordsContainer;
    @FXML private Label            countLabel;

    private List<FileRecord> allRecords = new ArrayList<>();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn() ||
                !PermissionService.canViewAllRecords(Session.getUser().getRole())) {
            showError("Access denied: insufficient permissions.");
            return;
        }
        statusFilter.getItems().addAll("All", "READY", "VIEWED", "ARCHIVED");
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        loadRecords();
    }

    @FXML
    private void handleRefresh(ActionEvent e) { loadRecords(); }

    private void loadRecords() {
        try {
            allRecords = FileService.listAll();
            applyFilter();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to load records: " + ex.getMessage());
        }
    }

    private void applyFilter() {
        String search = searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        List<FileRecord> filtered = allRecords.stream()
            .filter(r -> {
                if (!"All".equals(status) && !status.equals(r.getStatus())) return false;
                if (!search.isEmpty()) {
                    String hay = (nullStr(r.getPatientName()) + " " +
                                  nullStr(r.getPatientNumber()) + " " +
                                  nullStr(r.getTestType()) + " " +
                                  nullStr(r.getDoctorName())).toLowerCase();
                    return hay.contains(search);
                }
                return true;
            }).toList();

        countLabel.setText(filtered.size() + " record" + (filtered.size() == 1 ? "" : "s"));
        renderRecords(filtered);
    }

    private void renderRecords(List<FileRecord> records) {
        recordsContainer.getChildren().clear();
        boolean canExport = Session.hasRole(Role.ADMIN);

        if (records.isEmpty()) {
            Label empty = new Label("No records match your search.");
            empty.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:13px; -fx-padding:32 0;");
            recordsContainer.getChildren().add(empty);
            return;
        }
        for (FileRecord r : records) recordsContainer.getChildren().add(buildRow(r, canExport));
    }

    private HBox buildRow(FileRecord r, boolean canExport) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("record-row");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(nullStr(r.getPatientName(), "Unknown Patient"));
        name.getStyleClass().add("record-patient-name");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label pNum    = new Label("#" + nullStr(r.getPatientNumber()));
        pNum.getStyleClass().add("record-patient-num");
        Label sep1    = new Label("·"); sep1.setStyle("-fx-text-fill:#D1D5DB;");
        Label testType = new Label(nullStr(r.getTestType(), "—"));
        testType.getStyleClass().add("record-meta");
        Label sep2    = new Label("·"); sep2.setStyle("-fx-text-fill:#D1D5DB;");
        Label date    = new Label(r.getTestDate() != null ? r.getTestDate().format(DATE_FMT) : "—");
        date.getStyleClass().add("record-meta");
        Label sep3    = new Label("·"); sep3.setStyle("-fx-text-fill:#D1D5DB;");
        Label doctor  = new Label(nullStr(r.getDoctorName(), "—"));
        doctor.getStyleClass().add("record-meta");

        metaRow.getChildren().addAll(pNum, sep1, testType, sep2, date, sep3, doctor);
        info.getChildren().addAll(name, metaRow);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label badge = new Label(nullStr(r.getStatus(), "—"));
        badge.getStyleClass().add(badgeClass(r.getStatus()));
        actions.getChildren().add(badge);

        if (canExport) {
            Button exportBtn = new Button("Export");
            exportBtn.getStyleClass().add("btn-ghost");
            exportBtn.setStyle("-fx-font-size:11px; -fx-padding:5 12;");
            exportBtn.setOnAction(e -> handleExport(r.getFileId(), exportBtn));
            actions.getChildren().add(exportBtn);
        }

        row.getChildren().addAll(info, actions);
        return row;
    }

    private void handleExport(int fileId, Button btn) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Folder");
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);

        Stage stage = (Stage) btn.getScene().getWindow();
        File dir = chooser.showDialog(stage);
        if (dir == null) return;

        btn.setDisable(true);
        btn.setText("…");

        Thread t = new Thread(() -> {
            try {
                File out = FileService.exportDecrypted(fileId, dir);
                javafx.application.Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Export");
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setHeaderText(null);
                    a.setContentText("Exported to:\n" + out.getAbsolutePath());
                    a.showAndWait();
                    try { java.awt.Desktop.getDesktop().open(out); } catch (Exception ignored) {}
                    loadRecords();
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Export");
                    showError("Export failed: " + ex.getMessage());
                });
            }
        }, "smls-export");
        t.setDaemon(true);
        t.start();
    }

    private static String badgeClass(String status) {
        if (status == null) return "badge-pending";
        return switch (status.toUpperCase()) {
            case "READY"    -> "badge-ready";
            case "VIEWED"   -> "badge-viewed";
            case "ARCHIVED" -> "badge-archived";
            default         -> "badge-pending";
        };
    }

    private static String nullStr(String s)          { return s != null ? s : ""; }
    private static String nullStr(String s, String d) { return (s != null && !s.isBlank()) ? s : d; }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
