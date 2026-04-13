package javafxapplication7;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafxapplication7.model.Role;
import javafxapplication7.service.FileService;
import javafxapplication7.session.Session;

public class HomePageController {

    @FXML private Label lblWelcome;
    @FXML private Label lblRole;
    @FXML private HBox  statCardsRow;
    @FXML private VBox  quickActionsBox;

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn()) return;

        String name = Session.getUser().getFullName();
        String role = Session.getUser().getRole().getDisplayName();

        if (lblWelcome != null) lblWelcome.setText("Welcome, " + name);
        if (lblRole    != null) lblRole.setText(role + " — " + greeting());

        buildQuickActions();
        loadStatCardsAsync();   // async: never blocks the FX thread
    }

    // ── Stat cards (loaded on a background thread) ────────────────────────────

    private void loadStatCardsAsync() {
        if (statCardsRow == null) return;
        Role role = Session.getUser().getRole();

        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                return switch (role) {
                    case ADMIN -> new int[]{
                        FileService.countAll(),
                        FileService.countByStatus("READY"),
                        FileService.countByStatus("VIEWED"),
                        FileService.countByStatus("ARCHIVED")
                    };
                    case RECEPTIONIST -> new int[]{
                        FileService.countAll(),
                        FileService.countByUploader(Session.getUser().getUserId()),
                        FileService.countByStatus("READY")
                    };
                    case DOCTOR -> new int[]{
                        FileService.countAll(),
                        FileService.countByStatus("READY"),
                        FileService.countByStatus("VIEWED")
                    };
                };
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> buildStatCards(role, task.getValue())));
        task.setOnFailed   (e -> { /* counts stay blank on DB error — acceptable */ });

        Thread t = new Thread(task, "smls-dashboard-stats");
        t.setDaemon(true);
        t.start();
    }

    private void buildStatCards(Role role, int[] counts) {
        statCardsRow.getChildren().clear();

        switch (role) {
            case ADMIN -> statCardsRow.getChildren().addAll(
                statCard("Total Records",   counts[0], "stat-value-blue"),
                statCard("Ready",           counts[1], "stat-value-green"),
                statCard("Viewed",          counts[2], "stat-value-purple"),
                statCard("Archived",        counts[3], "stat-value-amber")
            );
            case RECEPTIONIST -> statCardsRow.getChildren().addAll(
                statCard("Total Records", counts[0], "stat-value-blue"),
                statCard("My Uploads",   counts[1], "stat-value-green"),
                statCard("Ready",        counts[2], "stat-value-amber")
            );
            case DOCTOR -> statCardsRow.getChildren().addAll(
                statCard("Total Records",    counts[0], "stat-value-blue"),
                statCard("Awaiting Review",  counts[1], "stat-value-green"),
                statCard("Reviewed",         counts[2], "stat-value-purple")
            );
        }
    }

    private static VBox statCard(String labelText, int value, String valueStyle) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("stat-label");

        Label val = new Label(String.valueOf(value));
        val.getStyleClass().add(valueStyle);

        card.getChildren().addAll(lbl, val);
        return card;
    }

    // ── Quick actions ─────────────────────────────────────────────────────────

    private void buildQuickActions() {
        if (quickActionsBox == null) return;
        quickActionsBox.getChildren().clear();

        switch (Session.getUser().getRole()) {
            case ADMIN -> quickActionsBox.getChildren().addAll(
                actionBtn("&#8679;  New Record",  "btn-success",
                          e -> MainLayoutController.navigateToUpload()),
                actionBtn("&#9776;  All Records", "btn-primary",
                          e -> MainLayoutController.navigateToRecords())
            );
            case RECEPTIONIST -> quickActionsBox.getChildren().addAll(
                actionBtn("&#8679;  New Record",      "btn-success",
                          e -> MainLayoutController.navigateToUpload()),
                actionBtn("&#9776;  My Uploads",      "btn-primary",
                          e -> MainLayoutController.navigateToRecords())
            );
            case DOCTOR -> quickActionsBox.getChildren().add(
                actionBtn("&#128196;  Patient Files", "btn-primary",
                          e -> MainLayoutController.navigateToPatientFiles())
            );
        }
    }

    private static Button actionBtn(String text, String styleClass,
                                    javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        btn.setPrefHeight(40);
        btn.setMinWidth(180);
        btn.setOnAction(handler);
        return btn;
    }

    private static String greeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }
}
