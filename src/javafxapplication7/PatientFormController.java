package javafxapplication7;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class PatientFormController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private DatePicker dobPicker;
    @FXML
    private TextField patientNumberField;
    @FXML
    private Label fileNameLabel;

    @FXML
    private ComboBox<String> testNameBox;
    @FXML
    private ComboBox<String> doctorBox;
    @FXML
    private ComboBox<String> technicianBox;
    @FXML
    private DatePicker testDatePicker;
    @FXML
    private ComboBox<String> statusBox;

    private byte[] encryptedFile;

    public void setEncryptedFileData(byte[] encryptedData) {
        this.encryptedFile = encryptedData;
        fileNameLabel.setText("Encrypted PDF (" + encryptedData.length + " bytes)");
    }

    @FXML
    public void initialize() {
        testNameBox.getItems().addAll("Blood Test", "X-Ray", "MRI", "CT Scan", "COVID-19 PCR");
        doctorBox.getItems().addAll("Dr. Smith", "Dr. Brown", "Dr. Johnson");
        technicianBox.getItems().addAll("Tech A", "Tech B", "Tech C");
        statusBox.getItems().addAll("Pending", "In Progress", "Completed");
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        LocalDate dob = dobPicker.getValue();
        String patientNumber = patientNumberField.getText().trim();

        String testName = testNameBox.getValue();
        String doctorName = doctorBox.getValue();
        String technicianName = technicianBox.getValue();
        LocalDate testDate = testDatePicker.getValue();
        String status = statusBox.getValue();

        // Validate — report the first missing field by name for clarity
        if (firstName.isEmpty())    { showAlert(Alert.AlertType.WARNING, "First name is required.");            return; }
        if (lastName.isEmpty())     { showAlert(Alert.AlertType.WARNING, "Last name is required.");             return; }
        if (dob == null)            { showAlert(Alert.AlertType.WARNING, "Date of birth is required.");         return; }
        if (patientNumber.isEmpty()){ showAlert(Alert.AlertType.WARNING, "Patient number is required.");        return; }
        if (testName == null)       { showAlert(Alert.AlertType.WARNING, "Please select a test type.");         return; }
        if (doctorName == null)     { showAlert(Alert.AlertType.WARNING, "Please select a doctor.");            return; }
        if (technicianName == null) { showAlert(Alert.AlertType.WARNING, "Please select a technician.");        return; }
        if (testDate == null)       { showAlert(Alert.AlertType.WARNING, "Test date is required.");             return; }
        if (status == null)         { showAlert(Alert.AlertType.WARNING, "Please select a status.");            return; }
        if (encryptedFile == null)  { showAlert(Alert.AlertType.ERROR,   "No encrypted file found. Please encrypt a PDF first."); return; }

        if (encryptedFile.length > 1024 * 1024 * 10) { // 10 MB safety limit
            showAlert(Alert.AlertType.ERROR, "Encrypted file exceeds the 10 MB limit. Please use a smaller PDF.");
            return;
        }

        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            // 1. Insert/update patient
            String patientSql = "INSERT INTO patients (first_name, last_name, dob, patient_number) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (patient_number) DO UPDATE SET " +
                    "first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, dob = EXCLUDED.dob";
            try (PreparedStatement ps = conn.prepareStatement(patientSql)) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setDate(3, java.sql.Date.valueOf(dob));
                ps.setString(4, patientNumber);
                ps.executeUpdate();
            }

            // 2. Related keys
            int testId = insertOrGetId(conn, "tests", "test_id", "test_name", testName);
            int doctorId = insertOrGetId(conn, "doctors", "doctor_id", "doctor_name", doctorName);
            int technicianId = insertOrGetId(conn, "technicians", "technician_id", "technician_name", technicianName);

            // 3. Insert test record
            String recordSql = "INSERT INTO test_records (patient_number, test_id, doctor_id, technician_id, test_date, test_status, encrypted_file) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(recordSql)) {
                ps.setString(1, patientNumber);
                ps.setInt(2, testId);
                ps.setInt(3, doctorId);
                ps.setInt(4, technicianId);
                ps.setDate(5, java.sql.Date.valueOf(testDate));
                ps.setString(6, status);
                ps.setBytes(7, encryptedFile);
                ps.executeUpdate();
            }

            conn.commit();
            showAlert(Alert.AlertType.INFORMATION, "✅ All patient data and file saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "❌ Failed to save data: " + e.getMessage());
        }
    }


    private int insertOrGetId(Connection conn, String table, String idColumn, String nameColumn, String value) throws SQLException {
        String query = "SELECT " + idColumn + " FROM " + table + " WHERE " + nameColumn + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(idColumn);
        }

        // Use RETURNING to get the generated key (PostgreSQL syntax)
        String insert = "INSERT INTO " + table + " (" + nameColumn + ") VALUES (?) RETURNING " + idColumn;
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }

        throw new SQLException("Unable to insert or retrieve ID for value: " + value);
    }

    @FXML
    private void handleCancel(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/javafxapplication7/openEncryption.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    @FXML
    private void handleRandomFill(ActionEvent event) {
        firstNameField.setText("John");
        lastNameField.setText("Doe");
        dobPicker.setValue(LocalDate.of(1995, 5, 15));
        patientNumberField.setText("P" + (int) (Math.random() * 10000));
        testNameBox.setValue("Blood Test");
        doctorBox.setValue("Dr. Smith");
        technicianBox.setValue("Tech A");
        testDatePicker.setValue(LocalDate.now());
        statusBox.setValue("Pending");
        showAlert(Alert.AlertType.INFORMATION, "🔄 Form pre-filled with random data.");
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
