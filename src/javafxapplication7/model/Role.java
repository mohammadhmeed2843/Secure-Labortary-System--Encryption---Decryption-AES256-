package javafxapplication7.model;

public enum Role {
    ADMIN("Admin"),
    TECHNICIAN("Technician"),
    DOCTOR("Doctor");

    private final String displayName;

    Role(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
