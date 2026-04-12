package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafxapplication7.model.FileRecord;
import javafxapplication7.service.FileService;
import javafxapplication7.session.Session;

import java.io.File;
import java.util.List;

/**
 * Controller for the "Export Medical File" screen.
 *
 * Loads all accessible records, lets the user pick one, choose an output folder,
 * and click Export. FileService handles all decryption transparently.
 * No keys, no IVs, no cipher code belongs here.
 */
public class OpenDecryptionController {

    @FXML private ComboBox<String> patientComboBox;
    @FXML private TextField        outputFolderField;

    private List<FileRecord> records;
    private int              selectedFileId = -1;

    @FXML
    public void initialize() {
        try {
            // DOCTOR sees only READY files; ADMIN sees all
            if (Session.hasRole(javafxapplication7.model.Role.DOCTOR)) {
                records = FileService.listAll().stream()
                        .filter(r -> "READY".equals(r.getStatus()))
                        .toList();
            } else {
                records = FileService.listAll();
            }
            for (FileRecord r : records) patientComboBox.getItems().add(r.toDisplayString());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Failed to load records: " + e.getMessage());
        }
    }

    @FXML
    private void handleRecordSelect(ActionEvent event) {
        int idx = patientComboBox.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < records.size()) {
            selectedFileId = records.get(idx).getFileId();
        }
    }

    @FXML
    private void handleSelectFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Folder");
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);
        File dir = chooser.showDialog(new Stage());
        if (dir != null) outputFolderField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (selectedFileId < 0) {
            showAlert(Alert.AlertType.WARNING, "Please select a record first.");
            return;
        }
        if (outputFolderField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please select an output folder.");
            return;
        }

        try {
            File outDir  = new File(outputFolderField.getText().trim());
            File outFile = FileService.exportDecrypted(selectedFileId, outDir);
            showAlert(Alert.AlertType.INFORMATION,
                    "File exported successfully:\n" + outFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
