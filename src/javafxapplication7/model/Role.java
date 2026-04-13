package javafxapplication7.model;

/**
 * Defines the three user roles in the Secure Medical Lab System.
 *
 *  RECEPTIONIST — Daily operator. Registers patients, uploads encrypted lab
 *                 reports. Cannot view or export file content.
 *
 *  DOCTOR       — Clinical viewer. Searches patients, opens decrypted reports.
 *                 Cannot upload or manage records.
 *
 *  ADMIN        — System guardian. Not a daily user. Manages accounts,
 *                 monitors system health, can archive records. All permissions.
 */
public enum Role {

    RECEPTIONIST("Receptionist"),
    DOCTOR("Doctor"),
    ADMIN("Admin");

    private final String displayName;

    Role(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
