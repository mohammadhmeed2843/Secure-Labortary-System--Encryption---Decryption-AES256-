package javafxapplication7.ui;

import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafxapplication7.models.FileRecord;
import javafxapplication7.models.Patient;
import javafxapplication7.services.FileService;
import javafxapplication7.services.PatientService;
import javafxapplication7.services.PermissionService;
import javafxapplication7.session.Session;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PatientFilesController {

    @FXML private TextField searchPatientField;
    @FXML private VBox      patientListContainer;
    @FXML private Label     selectedPatientLabel;
    @FXML private VBox      filesContainer;
    @FXML private Label     filesHeaderLabel;

    private List<Patient> allPatients = new ArrayList<>();
    private Patient       activePatient;
    private HBox          activeCard;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final String[] AVATAR_COLORS = {
        "#C41230","#2563EB","#059669","#7C3AED","#D97706","#DB2777","#0D9488","#EA580C"
    };

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn() ||
                !PermissionService.canViewPatientFiles(Session.getUser().getRole())) {
            if (filesHeaderLabel != null) filesHeaderLabel.setText("Access denied.");
            return;
        }
        searchPatientField.textProperty().addListener((obs, o, n) -> filterPatients(n));
        loadPatientsAsync();
    }

    @FXML
    private void handleRefreshPatients(ActionEvent e) {
        activePatient = null;
        activeCard    = null;
        clearFilePanel();
        loadPatientsAsync();
    }

    private void loadPatientsAsync() {
        patientListContainer.getChildren().clear();
        Label loading = new Label("Loading patients…");
        loading.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:12px; -fx-padding:20 16;");
        patientListContainer.getChildren().add(loading);

        Task<List<Patient>> task = new Task<>() {
            @Override protected List<Patient> call() throws Exception {
                return PatientService.listAll();
            }
        };
        task.setOnSucceeded(ev -> javafx.application.Platform.runLater(() -> {
            allPatients = task.getValue();
            renderPatientList(allPatients);
        }));
        task.setOnFailed(ev -> javafx.application.Platform.runLater(() -> {
            patientListContainer.getChildren().clear();
            Label err = new Label("Could not load patients.");
            err.setStyle("-fx-text-fill:#C41230; -fx-font-size:12px; -fx-padding:16;");
            patientListContainer.getChildren().add(err);
        }));
        Thread t = new Thread(task, "smls-load-patients");
        t.setDaemon(true);
        t.start();
    }

    private void filterPatients(String query) {
        if (query == null || query.isBlank()) { renderPatientList(allPatients); return; }
        String q = query.toLowerCase();
        renderPatientList(allPatients.stream()
            .filter(p -> (p.getFirstName() + " " + p.getLastName() + " " + p.getPatientNumber())
                          .toLowerCase().contains(q))
            .toList());
    }

    private void renderPatientList(List<Patient> patients) {
        patientListContainer.getChildren().clear();
        if (patients.isEmpty()) {
            patientListContainer.getChildren().add(buildEmptyPatientState());
            return;
        }
        for (int i = 0; i < patients.size(); i++) {
            HBox card = buildPatientCard(patients.get(i));
            card.setOpacity(0);
            card.setTranslateX(-18);
            patientListContainer.getChildren().add(card);

            FadeTransition ft = new FadeTransition(Duration.millis(260), card);
            ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(260), card);
            tt.setToX(0);
            ParallelTransition pt = new ParallelTransition(card, ft, tt);
            pt.setDelay(Duration.millis(i * 48L));
            pt.setInterpolator(Interpolator.EASE_OUT);
            pt.play();
        }
    }

    private HBox buildPatientCard(Patient p) {
        boolean isActive = activePatient != null &&
                           activePatient.getPatientNumber().equals(p.getPatientNumber());
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);
        setCardStyle(card, isActive);

        Label avatar = new Label(getInitials(p.getFirstName(), p.getLastName()));
        avatar.setStyle(
            "-fx-background-color:" + avatarColor(p.getPatientNumber()) + ";" +
            "-fx-text-fill:white; -fx-background-radius:50%;" +
            "-fx-min-width:42; -fx-min-height:42; -fx-max-width:42; -fx-max-height:42;" +
            "-fx-alignment:center; -fx-font-weight:bold; -fx-font-size:14px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.getFirstName() + " " + p.getLastName());
        name.setStyle("-fx-text-fill:" + (isActive ? "#C41230" : "#111827") +
                      "; -fx-font-size:13px; -fx-font-weight:bold;");
        Label pid = new Label("ID: " + p.getPatientNumber());
        pid.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:11px;");
        info.getChildren().addAll(name, pid);

        Label arrow = new Label("›");
        arrow.setStyle("-fx-text-fill:" + (isActive ? "#C41230" : "#D1D5DB") + "; -fx-font-size:20px;");

        card.getChildren().addAll(avatar, info, arrow);
        if (isActive) activeCard = card;

        card.setOnMouseEntered(ev -> {
            if (activeCard != card) {
                card.setStyle("-fx-background-color:#FEF2F2;" +
                              "-fx-border-color:#F3F4F6 transparent transparent transparent;" +
                              "-fx-border-width:0 0 1 0; -fx-padding:12 14 12 14;");
                name.setStyle("-fx-text-fill:#C41230; -fx-font-size:13px; -fx-font-weight:bold;");
                arrow.setStyle("-fx-text-fill:#C41230; -fx-font-size:20px;");
            }
        });
        card.setOnMouseExited(ev -> {
            if (activeCard != card) {
                setCardStyle(card, false);
                name.setStyle("-fx-text-fill:#111827; -fx-font-size:13px; -fx-font-weight:bold;");
                arrow.setStyle("-fx-text-fill:#D1D5DB; -fx-font-size:20px;");
            }
        });
        card.setOnMouseClicked(ev -> selectPatient(p, card, name, arrow));
        return card;
    }

    private void setCardStyle(HBox card, boolean active) {
        if (active) {
            card.setStyle("-fx-background-color:#FEF2F2;" +
                          "-fx-border-color:#FECACA transparent transparent transparent;" +
                          "-fx-border-width:0 0 1 0; -fx-padding:12 14 12 14;");
        } else {
            card.setStyle("-fx-background-color:transparent;" +
                          "-fx-border-color:#F3F4F6 transparent transparent transparent;" +
                          "-fx-border-width:0 0 1 0; -fx-padding:12 14 12 14;");
        }
    }

    private void selectPatient(Patient p, HBox card, Label nameLabel, Label arrowLabel) {
        if (activeCard != null && activeCard != card) {
            setCardStyle(activeCard, false);
            if (activeCard.getChildren().size() > 1 &&
                    activeCard.getChildren().get(1) instanceof VBox oldInfo &&
                    !oldInfo.getChildren().isEmpty() &&
                    oldInfo.getChildren().get(0) instanceof Label oldName) {
                oldName.setStyle("-fx-text-fill:#111827; -fx-font-size:13px; -fx-font-weight:bold;");
            }
            if (activeCard.getChildren().size() > 2 &&
                    activeCard.getChildren().get(2) instanceof Label oldArrow) {
                oldArrow.setStyle("-fx-text-fill:#D1D5DB; -fx-font-size:20px;");
            }
        }
        setCardStyle(card, true);
        nameLabel.setStyle("-fx-text-fill:#C41230; -fx-font-size:13px; -fx-font-weight:bold;");
        arrowLabel.setStyle("-fx-text-fill:#C41230; -fx-font-size:20px;");
        activeCard    = card;
        activePatient = p;

        ScaleTransition st1 = new ScaleTransition(Duration.millis(120), card);
        st1.setToX(1.02); st1.setToY(1.02);
        ScaleTransition st2 = new ScaleTransition(Duration.millis(120), card);
        st2.setToX(1.0); st2.setToY(1.0);
        new SequentialTransition(st1, st2).play();

        selectedPatientLabel.setText(p.getFirstName() + " " + p.getLastName());
        loadFilesForPatient(p.getPatientNumber());
    }

    private void loadFilesForPatient(String patientNumber) {
        filesContainer.getChildren().clear();
        filesHeaderLabel.setText("Loading…");
        try {
            List<FileRecord> records = FileService.listForPatient(patientNumber).stream()
                    .filter(r -> "READY".equals(r.getStatus()) || "VIEWED".equals(r.getStatus()))
                    .toList();

            if (records.isEmpty()) {
                filesHeaderLabel.setText("No results");
                filesContainer.getChildren().add(buildEmptyFileState());
                return;
            }
            filesHeaderLabel.setText(records.size() + " result" + (records.size() == 1 ? "" : "s"));
            for (int i = 0; i < records.size(); i++) {
                Node card = buildFileCard(records.get(i));
                card.setOpacity(0);
                filesContainer.getChildren().add(card);
                FadeTransition ft = new FadeTransition(Duration.millis(280), card);
                ft.setToValue(1);
                ft.setDelay(Duration.millis(i * 65L));
                ft.play();
            }
        } catch (Exception ex) {
            filesHeaderLabel.setText("Error loading files");
            Label err = new Label("Error: " + ex.getMessage());
            err.setStyle("-fx-text-fill:#C41230; -fx-font-size:12px;");
            filesContainer.getChildren().add(err);
        }
    }

    private HBox buildFileCard(FileRecord r) {
        HBox wrapper = new HBox(0);
        wrapper.setStyle(
            "-fx-background-color:#FFFFFF; -fx-background-radius:14;" +
            "-fx-border-color:#E5E7EB; -fx-border-radius:14; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,4);");

        VBox accent = new VBox();
        accent.setMinWidth(6); accent.setMaxWidth(6);
        accent.setStyle("-fx-background-color:" + statusColor(r.getStatus()) +
                        "; -fx-background-radius:14 0 0 14;");

        VBox content = new VBox(12);
        content.setStyle("-fx-padding:18 22;");
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(testIcon(r.getTestType()));
        iconLabel.setStyle("-fx-font-size:22px; -fx-background-color:" +
                           statusColor(r.getStatus()) + "22; -fx-background-radius:10; -fx-padding:8;");

        VBox titleArea = new VBox(3);
        HBox.setHgrow(titleArea, Priority.ALWAYS);
        Label testName = new Label(r.getTestType() != null ? r.getTestType() : "Lab Report");
        testName.setStyle("-fx-text-fill:#111827; -fx-font-size:15px; -fx-font-weight:bold;");
        Label statusLbl = new Label(humanStatus(r.getStatus()));
        statusLbl.setStyle("-fx-text-fill:" + statusColor(r.getStatus()) +
                           "; -fx-font-size:11px; -fx-font-weight:bold;");
        titleArea.getChildren().addAll(testName, statusLbl);
        top.getChildren().addAll(iconLabel, titleArea);

        HBox meta = new HBox(24);
        meta.setAlignment(Pos.CENTER_LEFT);
        if (r.getTestDate() != null) {
            Label d = new Label("\uD83D\uDCC5  " + r.getTestDate().format(DATE_FMT));
            d.setStyle("-fx-text-fill:#6B7280; -fx-font-size:12px;");
            meta.getChildren().add(d);
        }
        if (r.getDoctorName() != null && !r.getDoctorName().isBlank()) {
            Label doc = new Label("\uD83D\uDC68\u200D⚕️  " + r.getDoctorName());
            doc.setStyle("-fx-text-fill:#6B7280; -fx-font-size:12px;");
            meta.getChildren().add(doc);
        }
        if (r.getOriginalName() != null && !r.getOriginalName().isBlank()) {
            Label fn = new Label("\uD83D\uDCC4  " + r.getOriginalName());
            fn.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:11px;");
            meta.getChildren().add(fn);
        }

        HBox actionRow = new HBox();
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button btn = new Button("   Open Report   ");
        btn.setStyle("-fx-background-color:#C41230; -fx-text-fill:white;" +
                     "-fx-font-size:13px; -fx-font-weight:bold;" +
                     "-fx-background-radius:9; -fx-padding:10 24; -fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color:#A30E27; -fx-text-fill:white;" +
            "-fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-radius:9; -fx-padding:10 24; -fx-cursor:hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color:#C41230; -fx-text-fill:white;" +
            "-fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-radius:9; -fx-padding:10 24; -fx-cursor:hand;"));

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(20, 20);
        pi.setStyle("-fx-progress-color:#C41230;");
        pi.setVisible(false);

        HBox btnGroup = new HBox(8);
        btnGroup.setAlignment(Pos.CENTER_RIGHT);
        btnGroup.getChildren().addAll(pi, btn);
        actionRow.getChildren().add(btnGroup);

        btn.setOnAction(e -> {
            btn.setDisable(true);
            btn.setText("Opening…");
            pi.setVisible(true);
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "smls_view");
            Thread t = new Thread(() -> {
                try {
                    File pdf = FileService.exportDecrypted(r.getFileId(), tempDir);
                    pdf.deleteOnExit();
                    javafx.application.Platform.runLater(() -> {
                        pi.setVisible(false);
                        btn.setDisable(false);
                        btn.setText("   Open Report   ");
                        try { java.awt.Desktop.getDesktop().open(pdf); }
                        catch (Exception ex) { showError("Cannot open viewer: " + ex.getMessage()); }
                        if (activePatient != null)
                            loadFilesForPatient(activePatient.getPatientNumber());
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        pi.setVisible(false);
                        btn.setDisable(false);
                        btn.setText("   Open Report   ");
                        showError("Failed: " + ex.getMessage());
                    });
                }
            }, "smls-view");
            t.setDaemon(true);
            t.start();
        });

        content.getChildren().addAll(top, meta, actionRow);
        wrapper.getChildren().addAll(accent, content);
        return wrapper;
    }

    private VBox buildEmptyPatientState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding:40 20;");
        Label icon = new Label("\uD83D\uDD0D");
        icon.setStyle("-fx-font-size:32px;");
        Label msg = new Label("No patients found");
        msg.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:13px;");
        box.getChildren().addAll(icon, msg);
        return box;
    }

    private VBox buildEmptyFileState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding:70 20;");
        Label icon = new Label("\uD83D\uDCC2");
        icon.setStyle("-fx-font-size:48px;");
        Label msg = new Label("No test results yet");
        msg.setStyle("-fx-text-fill:#374151; -fx-font-size:16px; -fx-font-weight:bold;");
        Label sub = new Label("No lab reports have been uploaded\nfor this patient yet.");
        sub.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:12px;");
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);
        box.getChildren().addAll(icon, msg, sub);
        return box;
    }

    private void clearFilePanel() {
        selectedPatientLabel.setText("Select a patient");
        filesHeaderLabel.setText("No patient selected");
        filesContainer.getChildren().clear();
    }

    private static String getInitials(String first, String last) {
        String f = (first != null && !first.isBlank()) ? String.valueOf(first.trim().charAt(0)) : "";
        String l = (last  != null && !last.isBlank())  ? String.valueOf(last.trim().charAt(0))  : "";
        return (f + l).toUpperCase();
    }

    private static String avatarColor(String pn) {
        int h = pn != null ? Math.abs(pn.hashCode()) : 0;
        return AVATAR_COLORS[h % AVATAR_COLORS.length];
    }

    private static String statusColor(String status) {
        if (status == null) return "#D97706";
        return switch (status.toUpperCase()) {
            case "READY"    -> "#059669";
            case "VIEWED"   -> "#2563EB";
            case "ARCHIVED" -> "#9CA3AF";
            default         -> "#D97706";
        };
    }

    private static String humanStatus(String status) {
        if (status == null) return "Pending";
        return switch (status.toUpperCase()) {
            case "READY"  -> "✓ Ready to view";
            case "VIEWED" -> "● Viewed";
            default       -> status;
        };
    }

    private static String testIcon(String t) {
        if (t == null) return "\uD83E\uDDEA";
        String tl = t.toLowerCase();
        if (tl.contains("blood") || tl.contains("cbc") || tl.contains("lipid")) return "\uD83E\uDE78";
        if (tl.contains("x-ray") || tl.contains("xray") || tl.contains("mri") || tl.contains("ct")) return "\uD83E\uDE7B";
        if (tl.contains("urine") || tl.contains("urinalysis")) return "\uD83D\uDD2C";
        if (tl.contains("thyroid") || tl.contains("hormone")) return "⚗️";
        if (tl.contains("covid") || tl.contains("pcr")) return "\uD83E\uDDA0";
        return "\uD83E\uDDEA";
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
