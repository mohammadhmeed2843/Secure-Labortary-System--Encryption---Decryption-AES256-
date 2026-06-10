package javafxapplication7.services;

import javafxapplication7.models.Role;

/**
 * Single source of truth for all role-based permission rules.
 * Every access-control decision flows through this class.
 */
public final class PermissionService {

    /** RECEPTIONIST and ADMIN can upload. DOCTOR cannot. */
    public static boolean canUpload(Role role) {
        return role == Role.RECEPTIONIST || role == Role.ADMIN;
    }

    /** DOCTOR and ADMIN can export/decrypt. RECEPTIONIST cannot. */
    public static boolean canExport(Role role) {
        return role == Role.DOCTOR || role == Role.ADMIN;
    }

    /** RECEPTIONIST and ADMIN can view the full records list. */
    public static boolean canViewAllRecords(Role role) {
        return role == Role.RECEPTIONIST || role == Role.ADMIN;
    }

    /** DOCTOR and ADMIN can search the patient-files view. */
    public static boolean canViewPatientFiles(Role role) {
        return role == Role.DOCTOR || role == Role.ADMIN;
    }

    /** ADMIN only can manage user accounts and system settings. */
    public static boolean canManageUsers(Role role) {
        return role == Role.ADMIN;
    }

    /** ADMIN only can archive or restore records. */
    public static boolean canArchive(Role role) {
        return role == Role.ADMIN;
    }

    private PermissionService() {}
}
