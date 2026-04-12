package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;

public class HomePageController {
   
    /* ----------  FXML controls  ---------- */
    @FXML private TextField ivDisplayField;
    @FXML private TextField ikDisplayField;
    @FXML private TextField resultTextField;

    /* ----------  IV + Key generators  ---------- */
    @FXML
    private void IVGeneration(ActionEvent e) {
        byte[] ivBytes = new byte[8];        // 128‑bit IV
        new SecureRandom().nextBytes(ivBytes);
        String hex = toHex(ivBytes);
        ivDisplayField.setText(hex);
        updateCombinedField();
    }

    @FXML
    private void KeyGeneration(ActionEvent e) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey sk = kg.generateKey();
        String base64 = java.util.Base64.getEncoder().encodeToString(sk.getEncoded());
        ikDisplayField.setText(base64);
        updateCombinedField();
    }

    /* ----------  Clipboard + Save buttons  ---------- */
    @FXML private void copyIv () { copyToClipboard(ivDisplayField.getText()); }
    @FXML private void copyKey() { copyToClipboard(ikDisplayField.getText()); }

    @FXML private void saveIv () { CryptoStore.saveIv(ivDisplayField.getText()); }
    @FXML private void saveKey() { CryptoStore.saveKey(ikDisplayField.getText()); }

    /* ----------  Save IV+Key to user‑chosen text file  ---------- */
    @FXML
    private void saveToFile(ActionEvent e) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Keys");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files","*.txt"));
        File target = fc.showSaveDialog(stage(e));
        if (target != null) try (BufferedWriter bw=new BufferedWriter(new FileWriter(target))) {
            bw.write(resultTextField.getText());
        } catch(IOException ex){ ex.printStackTrace(); }
    }

    /* ----------  Navigation buttons  ---------- */
    @FXML
    private void openEncryption(ActionEvent e) throws IOException {
        switchScene("/javafxapplication7/openEncryption.fxml", e);
    }

    @FXML
    private void openDecryption(ActionEvent e) throws IOException {
        switchScene("/javafxapplication7/OpenDecryption.fxml", e);
    }

    /* ----------  Helpers  ---------- */
    private void updateCombinedField() {
        resultTextField.setText("IV:  " + ivDisplayField.getText() + "    "
                + "Key: " + ikDisplayField.getText());
    }

    private static String toHex(byte[] data){
        StringBuilder sb=new StringBuilder();
        for(byte b:data) sb.append(String.format("%02x",b));
        return sb.toString();
    }

    private static void copyToClipboard(String s){
        ClipboardContent cc = new ClipboardContent();
        cc.putString(s);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private void switchScene(String fxml, ActionEvent e) throws IOException{
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage st    = stage(e);
        st.setScene(new Scene(root));
        st.show();
    }

    private static Stage stage(ActionEvent e){
        return (Stage)((Node)e.getSource()).getScene().getWindow();
    }
}
