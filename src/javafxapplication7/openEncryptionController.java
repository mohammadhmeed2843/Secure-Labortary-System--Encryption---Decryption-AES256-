package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;

public class openEncryptionController implements Initializable {

    /* ----------  FXML fields  ---------- */
    @FXML private TextField inputFolderTextField;
    @FXML private TextField keyTextField;
    @FXML private TextField ivTextField;
    @FXML private TextField outputFolderTextField;

    /* ----------  internal state  ---------- */
    private final List<File> selectedPdfs = new ArrayList<>();
    private void show(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    /* ----------  browse PDF(s)  ---------- */
    @FXML
    private void browseInputFolder(ActionEvent e) {
        FileChooser ch = new FileChooser();
        ch.setTitle("Select PDF file(s)");
        ch.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        // Set Desktop as default location
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) {
            ch.setInitialDirectory(desktop);
        }

        List<File> files = ch.showOpenMultipleDialog(stage(e));
        if (files != null && !files.isEmpty()) {
            selectedPdfs.clear();
            selectedPdfs.addAll(files);
            inputFolderTextField.setText(
                    files.stream().map(File::getAbsolutePath)
                            .reduce((a, b) -> a + "; " + b).orElse(""));
        }
    }


    @FXML
    private void browseOutputFolder(ActionEvent e) {
        FileChooser ch = new FileChooser();
        ch.setTitle("Pick any filename inside target folder");

        // Set Desktop as default location
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) {
            ch.setInitialDirectory(desktop);
        }

        File dummy = ch.showSaveDialog(stage(e));
        if (dummy != null) {
            outputFolderTextField.setText(dummy.getParent());
        }
    }


    /* ----------  paste buttons  ---------- */
    @FXML private void pasteKey() { keyTextField.setText(CryptoStore.getKey()); }
    @FXML private void pasteIv () { ivTextField .setText(CryptoStore.getIv());  }

    /* ----------  encryption ---------- */
    @FXML
    private void encryptFileAndHold(ActionEvent event) {
        String base64Key = keyTextField.getText().trim();
        String ivString = ivTextField.getText().trim();

        if (selectedPdfs.isEmpty()) {
            show(Alert.AlertType.WARNING, "Please select a PDF file.");
            return;
        }

        try {
            File pdf = selectedPdfs.get(0); // Single-file assumption
            byte[] inputBytes = new byte[(int) pdf.length()];
            try (FileInputStream fis = new FileInputStream(pdf)) {
                fis.read(inputBytes);
            }

            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] ivBytes = ivString.getBytes();
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] encryptedData = cipher.doFinal(inputBytes);

            // ✅ Save encrypted file to output folder
            if (!outputFolderTextField.getText().trim().isEmpty()) {
                File outDir = new File(outputFolderTextField.getText().trim());
                if (!outDir.exists()) outDir.mkdirs();

                String originalName = pdf.getName().replaceFirst("(?i)\\.pdf$", "");
                File outputFile = new File(outDir, originalName + ".menc");

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(encryptedData);
                    System.out.println("✅ Encrypted file saved to: " + outputFile.getAbsolutePath());
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                    show(Alert.AlertType.ERROR, "⚠️ Failed to save .menc file to output folder.");
                }
            }

            show(Alert.AlertType.INFORMATION, "✅ PDF encrypted. Please fill in the patient record.");

            // Store encrypted data in CryptoStore; PatientFormController reads it from there
            CryptoStore.saveEncryptedFile(encryptedData);
            MainLayoutController.navigateTo("PatientForm.fxml");

        } catch (Exception e) {
            e.printStackTrace();
            show(Alert.AlertType.ERROR, "Encryption failed: " + e.getMessage());
        }
    }


    /* ----------  navigation ---------- */
    @FXML
    private void backToHomePage(ActionEvent e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/javafxapplication7/HomePage.fxml"));
        stage(e).setScene(new Scene(root));
    }

    /* ----------  helpers ---------- */
    private static void alert(String msg){ new Alert(Alert.AlertType.INFORMATION,msg).showAndWait(); }
    private static Stage stage(ActionEvent e){ return (Stage)((Node)e.getSource()).getScene().getWindow(); }
    public void initialize(URL u, ResourceBundle rb) {}
}
