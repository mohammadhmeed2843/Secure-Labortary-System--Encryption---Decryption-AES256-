package javafxapplication7;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafxapplication7.service.FileService;
import javafxapplication7.session.Session;

public class HomePageController {

    @FXML private Label lblWelcome;
    @FXML private Label lblRole;
    @FXML private Label lblTotalRecords;

    @FXML
    public void initialize() {
        if (Session.isLoggedIn()) {
            if (lblWelcome != null) lblWelcome.setText("Welcome, " + Session.getUser().getFullName());
            if (lblRole    != null) lblRole.setText(Session.getUser().getRole().getDisplayName());
        }
        if (lblTotalRecords != null) {
            try {
                int count = FileService.listAll().size();
                lblTotalRecords.setText(String.valueOf(count));
            } catch (Exception e) {
                lblTotalRecords.setText("—");
            }
        }
    }

    @FXML private void openUpload() { MainLayoutController.navigateToUpload(); }
    @FXML private void openExport() { MainLayoutController.navigateToExport(); }
}
