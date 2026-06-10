package javafxapplication7.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafxapplication7.models.Role;
import javafxapplication7.services.FileService;
import javafxapplication7.session.Session;

public class HomePageController {

    @FXML private Label lblWelcome;
    @FXML private Label lblRole;
    @FXML private HBox  statCardsRow;
    @FXML private HBox  quickActionsBox;

    @FXML
    public void initialize() {
        if (!Session.isLoggedIn()) return;

        String name = Session.getUser().getFullName();
        String role = Session.getUser().getRole().getDisplayName();

        if (lblWelcome != null) lblWelcome.setText("Welcome, " + name);
        if (lblRole    != null) lblRole.setText(role + " — " + greeting());

        buildQuickActions();
        loadStatCardsAsync();
    }

    private void loadStatCardsAsync() {
        if (statCardsRow == null) return;
        Role role = Session.getUser().getRole();

        Task<int[]> task = new Task<>() {
            @Override protected int[] call() {
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
        task.setOnFailed   (e -> { /* counts stay blank on DB error */ });

        Thread t = new Thread(task, "smls-dashboard-stats");
        t.setDaemon(true);
        t.start();
    }

    private void buildStatCards(Role role, int[] counts) {
        statCardsRow.getChildren().clear();
        switch (role) {
            case ADMIN -> statCardsRow.getChildren().addAll(
                statCard("Total Records", counts[0], "stat-value-blue"),
                statCard("Ready",         counts[1], "stat-value-green"),
                statCard("Viewed",        counts[2], "stat-value-purple"),
                statCard("Archived",      counts[3], "stat-value-amber")
            );
            case RECEPTIONIST -> statCardsRow.getChildren().addAll(
                statCard("Total Records", counts[0], "stat-value-blue"),
                statCard("My Uploads",   counts[1], "stat-value-green"),
                statCard("Ready",        counts[2], "stat-value-amber")
            );
            case DOCTOR -> statCardsRow.getChildren().addAll(
                statCard("Total Records",   counts[0], "stat-value-blue"),
                statCard("Awaiting Review", counts[1], "stat-value-green"),
                statCard("Reviewed",        counts[2], "stat-value-purple")
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

    private void buildQuickActions() {
        if (quickActionsBox == null) return;
        quickActionsBox.getChildren().clear();

        switch (Session.getUser().getRole()) {
            case ADMIN -> quickActionsBox.getChildren().addAll(
                actionBtn("\uD83D\uDC64  User Accounts", "btn-primary",
                          e -> MainLayoutController.navigateToAdminUsers()),
                actionBtn("\uD83D\uDCCB  Activity Log",  "btn-secondary",
                          e -> MainLayoutController.navigateToAdminAudit()),
                actionBtn("\uD83D\uDD04  File Recovery", "btn-secondary",
                          e -> MainLayoutController.navigateToAdminRecovery())
            );
            case RECEPTIONIST -> quickActionsBox.getChildren().addAll(
                actionBtn("⇧  New Test Record", "btn-success",
                          e -> MainLayoutController.navigateToUpload()),
                actionBtn("☰  All Records",     "btn-primary",
                          e -> MainLayoutController.navigateToRecords())
            );
            case DOCTOR -> quickActionsBox.getChildren().add(
                actionBtn("\uD83D\uDCC4  View Patient Files", "btn-primary",
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
