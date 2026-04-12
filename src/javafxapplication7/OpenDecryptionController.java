package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public class OpenDecryptionController {

    @FXML private ComboBox<String> patientComboBox;
    @FXML private TextField keyField;
    @FXML private TextField ivField;
    @FXML private TextField outputFolderField;
    @FXML
    private TextField ivTextField;
    @FXML
    private TextField keyTextField;

    private byte[] encryptedFileBytes;
    private String selectedPatientNumber;

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
            showAlert(Alert.AlertType.ERROR, "❌ Failed to load patient list: " + e.getMessage());
        }
    }

    @FXML
    private void handlePatientSelect(ActionEvent event) {
        String selected = patientComboBox.getValue();
        if (selected == null || !selected.contains("P#")) {
            showAlert(Alert.AlertType.WARNING, "⚠ Please select a valid patient.");
            return;
        }

        selectedPatientNumber = selected.split("P#")[1].replace(")", "").trim();

        try (Connection conn = DatabaseConnection.connect()) {
            String sql = "SELECT encrypted_file FROM test_records WHERE patient_number = ? ORDER BY test_date DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, selectedPatientNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    encryptedFileBytes = rs.getBytes("encrypted_file");
                    showAlert(Alert.AlertType.INFORMATION, "✅ Encrypted file loaded for patient.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "⚠ No encrypted file found for this patient.");
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "❌ Error loading file: " + e.getMessage());
        }
    }
    @FXML
    private void pasteKey() {
        keyTextField.setText(CryptoStore.getKey());
    }

    @FXML
    private void pasteIv() {
        ivTextField.setText(CryptoStore.getIv());
    }
    @FXML
    private void backToHomePage(ActionEvent e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/javafxapplication7/HomePage.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    @FXML
    private void handleSelectFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(new Stage());
        if (dir != null) {
            outputFolderField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void handleDecrypt(ActionEvent event) {
        if (encryptedFileBytes == null || keyTextField.getText().isEmpty() || ivTextField.getText().isEmpty() || outputFolderField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "❌ Please fill in all fields and ensure a file is loaded.");
            return;
        }

        try {
            byte[] key = Base64.getDecoder().decode(keyTextField.getText().trim());
            byte[] iv = ivTextField.getText().trim().getBytes();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(encryptedFileBytes);

            File outFile = new File(outputFolderField.getText(), "Decrypted_File.pdf");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(decrypted);
            }

            showAlert(Alert.AlertType.INFORMATION, "✅ File decrypted and saved to: " + outFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "❌ Decryption failed: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
