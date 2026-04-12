package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafxapplication7.session.Session;

import java.io.File;

/**
 * Controller for the "Upload Medical File" screen.
 *
 * Only responsibility: let the user pick a PDF and store it in Session.getDraft().
 * Encryption is triggered later by FileService when the patient form is submitted.
 * No keys, no IVs, no cipher code belongs here.
 */
public class openEncryptionController {

    @FXML private TextField inputFolderTextField;

    private File selectedFile;

    @FXML
    private void browseInputFolder(ActionEvent e) {
        FileChooser ch = new FileChooser();
        ch.setTitle("Select PDF File");
        ch.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) ch.setInitialDirectory(desktop);

        File file = ch.showOpenDialog(stage(e));
        if (file != null) {
            selectedFile = file;
            inputFolderTextField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void proceed(ActionEvent e) {
        if (selectedFile == null || !selectedFile.exists()) {
            show(Alert.AlertType.WARNING, "Please select a PDF file first.");
            return;
        }
        Session.getDraft().setOriginalFile(selectedFile);
        MainLayoutController.navigateTo("PatientForm.fxml");
    }

    private void show(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static Stage stage(ActionEvent e) {
        return (Stage) ((Node) e.getSource()).getScene().getWindow();
    }
}
