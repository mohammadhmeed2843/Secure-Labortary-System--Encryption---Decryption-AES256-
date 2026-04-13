package javafxapplication7;

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

        buildStatCards();
        buildQuickActions();
    }

    // ── Stat cards ────────────────────────────────────────────────────────────

    private void buildStatCards() {
        if (statCardsRow == null) return;
        statCardsRow.getChildren().clear();

        Role r = Session.getUser().getRole();

        if (r == Role.ADMIN) {
            int total    = FileService.countAll();
            int ready    = FileService.countByStatus("READY");
            int viewed   = FileService.countByStatus("VIEWED");
            int archived = FileService.countByStatus("ARCHIVED");

            statCardsRow.getChildren().addAll(
                statCard("Total Records",   String.valueOf(total),    "stat-value-blue"),
                statCard("Ready",           String.valueOf(ready),    "stat-value-green"),
                statCard("Viewed",          String.valueOf(viewed),   "stat-value-purple"),
                statCard("Archived",        String.valueOf(archived), "stat-value-amber")
            );

        } else if (r == Role.TECHNICIAN) {
            int total    = FileService.countAll();
            int mine     = FileService.countByUploader(Session.getUser().getUserId());
            int ready    = FileService.countByStatus("READY");

            statCardsRow.getChildren().addAll(
                statCard("Total Records", String.valueOf(total), "stat-value-blue"),
                statCard("My Uploads",   String.valueOf(mine),  "stat-value-green"),
                statCard("Ready",        String.valueOf(ready), "stat-value-amber")
            );

        } else if (r == Role.DOCTOR) {
            int total  = FileService.countAll();
            int ready  = FileService.countByStatus("READY");
            int viewed = FileService.countByStatus("VIEWED");

            statCardsRow.getChildren().addAll(
                statCard("Total Records", String.valueOf(total),  "stat-value-blue"),
                statCard("Awaiting Review", String.valueOf(ready), "stat-value-green"),
                statCard("Reviewed",      String.valueOf(viewed), "stat-value-purple")
            );
        }
    }

    private static VBox statCard(String labelText, String value, String valueStyle) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("stat-label");

        Label val = new Label(value);
        val.getStyleClass().add(valueStyle);

        card.getChildren().addAll(lbl, val);
        return card;
    }

    // ── Quick actions ─────────────────────────────────────────────────────────

    private void buildQuickActions() {
        if (quickActionsBox == null) return;
        quickActionsBox.getChildren().clear();

        Role r = Session.getUser().getRole();

        if (r == Role.ADMIN) {
            quickActionsBox.getChildren().addAll(
                actionBtn("&#8679;  New Record",     "btn-success", e -> MainLayoutController.navigateToUpload()),
                actionBtn("&#9776;  All Records",    "btn-primary", e -> MainLayoutController.navigateToRecords())
            );
        } else if (r == Role.TECHNICIAN) {
            quickActionsBox.getChildren().addAll(
                actionBtn("&#8679;  New Record",     "btn-success", e -> MainLayoutController.navigateToUpload()),
                actionBtn("&#9776;  View My Uploads","btn-primary", e -> MainLayoutController.navigateToRecords())
            );
        } else if (r == Role.DOCTOR) {
            quickActionsBox.getChildren().add(
                actionBtn("&#128196;  Patient Files","btn-primary", e -> MainLayoutController.navigateToPatientFiles())
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
