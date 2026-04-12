package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public class OpenDecryptionController {

    @FXML private ComboBox<String> patientComboBox;
    @FXML private TextField keyTextField;
    @FXML private TextField ivTextField;
    @FXML private TextField outputFolderField;

    private byte[] encryptedFileBytes;

    @FXML
    public void initialize() {
        try (Connection conn = DatabaseConnection.connect()) {
            String query = "SELECT patient_number, first_name, last_name FROM patients";
            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String display = rs.getString("first_name") + " " + rs.getString("last_name") +
                            " (P#" + rs.getString("patient_number") + ")";
                    patientComboBox.getItems().add(display);
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Failed to load patient list: " + e.getMessage());
        }
    }

    @FXML
    private void handlePatientSelect(ActionEvent event) {
        String selected = patientComboBox.getValue();
        if (selected == null || !selected.contains("P#")) {
            showAlert(Alert.AlertType.WARNING, "Please select a valid patient.");
            return;
        }

        String patientNumber = selected.split("P#")[1].replace(")", "").trim();

        try (Connection conn = DatabaseConnection.connect()) {
            String sql = "SELECT encrypted_file FROM test_records WHERE patient_number = ? ORDER BY test_date DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, patientNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    encryptedFileBytes = rs.getBytes("encrypted_file");
                    showAlert(Alert.AlertType.INFORMATION, "Encrypted file loaded for patient.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "No encrypted file found for this patient.");
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error loading file: " + e.getMessage());
        }
    }

    @FXML private void pasteKey() { keyTextField.setText(CryptoStore.getKey()); }
    @FXML private void pasteIv()  { ivTextField.setText(CryptoStore.getIv());   }

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
    private void handleDecrypt(ActionEvent event) {
        if (encryptedFileBytes == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a patient and load their file first.");
            return;
        }
        if (keyTextField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Decryption key is required.");
            return;
        }
        if (ivTextField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Initialization vector (IV) is required.");
            return;
        }
        if (outputFolderField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please select an output folder.");
            return;
        }

        try {
            byte[] key = Base64.getDecoder().decode(keyTextField.getText().trim());
            byte[] iv  = ivTextField.getText().trim().getBytes();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encryptedFileBytes);

            File outFile = new File(outputFolderField.getText().trim(), "Decrypted_File.pdf");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(decrypted);
            }

            showAlert(Alert.AlertType.INFORMATION, "File decrypted and saved to:\n" + outFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Decryption failed: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
