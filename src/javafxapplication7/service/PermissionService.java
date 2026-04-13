package javafxapplication7.service;

import javafxapplication7.model.Role;

/**
 * Single source of truth for all role-based permission rules.
 *
 * Every access-control decision in the application flows through this class.
 * When requirements change (e.g. "Doctors can now upload too"), only this
 * file needs to be edited — controllers and models stay untouched.
 *
 * Usage pattern:
 *   if (PermissionService.canUpload(session.getUser().getRole())) { ... }
 */
public final class PermissionService {

    /**
     * Can this role upload and encrypt lab files?
     * Receptionist: yes (their primary daily task).
     * Admin:        yes (override / recovery scenarios).
     * Doctor:       NO.
     */
    public static boolean canUpload(Role role) {
        return role == Role.RECEPTIONIST || role == Role.ADMIN;
    }

    /**
     * Can this role export (decrypt) lab files to disk?
     * Doctor:       yes (their primary task: read lab results).
     * Admin:        yes (audit / recovery).
     * Receptionist: NO — they upload, they do not read clinical content.
     */
    public static boolean canExport(Role role) {
        return role == Role.DOCTOR || role == Role.ADMIN;
    }

    /**
     * Can this role view the full list of all records?
     * Receptionist: yes (to manage their own uploads and general records).
     * Admin:        yes (full visibility).
     * Doctor:       NO — doctors use the patient-centric PatientFiles view.
     */
    public static boolean canViewAllRecords(Role role) {
        return role == Role.RECEPTIONIST || role == Role.ADMIN;
    }

    /**
     * Can this role access the patient-files view (search patients → view reports)?
     * Doctor:       yes (their primary interface).
     * Admin:        yes (audit).
     * Receptionist: NO.
     */
    public static boolean canViewPatientFiles(Role role) {
        return role == Role.DOCTOR || role == Role.ADMIN;
    }

    /**
     * Can this role manage user accounts and system settings?
     * Admin only.
     */
    public static boolean canManageUsers(Role role) {
        return role == Role.ADMIN;
    }

    /**
     * Can this role archive or permanently change record status?
     * Admin only.
     */
    public static boolean canArchive(Role role) {
        return role == Role.ADMIN;
    }

    private PermissionService() {}
}
