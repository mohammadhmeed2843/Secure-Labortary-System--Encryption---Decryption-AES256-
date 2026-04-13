package javafxapplication7.session;

import javafxapplication7.model.RecordDraft;
import javafxapplication7.model.Role;
import javafxapplication7.model.User;
import javafxapplication7.service.PermissionService;

/**
 * Application-wide session state.
 *
 * Holds the logged-in user and the active upload draft.
 * Both fields are volatile so that changes made on the JavaFX Application
 * Thread are immediately visible to background Task threads (and vice versa).
 *
 * Permission queries delegate to PermissionService — Session itself contains
 * no role-rule logic.
 */
public final class Session {

    private static volatile User        currentUser;
    private static volatile RecordDraft currentDraft;

    // ── Auth lifecycle ────────────────────────────────────────────────────────

    public static void login(User user) {
        currentUser  = user;
        currentDraft = null;
    }

    public static void logout() {
        currentUser  = null;
        currentDraft = null;
    }

    // ── User accessors ────────────────────────────────────────────────────────

    public static User    getUser()     { return currentUser;         }
    public static boolean isLoggedIn()  { return currentUser != null; }

    public static boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    // ── Permission shortcuts ──────────────────────────────────────────────────
    // Delegate to PermissionService with a null-safe guard.

    public static boolean canUpload() {
        return currentUser != null && PermissionService.canUpload(currentUser.getRole());
    }

    public static boolean canExport() {
        return currentUser != null && PermissionService.canExport(currentUser.getRole());
    }

    public static boolean canViewAllRecords() {
        return currentUser != null && PermissionService.canViewAllRecords(currentUser.getRole());
    }

    public static boolean canViewPatientFiles() {
        return currentUser != null && PermissionService.canViewPatientFiles(currentUser.getRole());
    }

    public static boolean canManageUsers() {
        return currentUser != null && PermissionService.canManageUsers(currentUser.getRole());
    }

    // ── Upload draft ──────────────────────────────────────────────────────────

    /**
     * Returns the active draft, creating a fresh one if none exists.
     * Accumulates file + patient + test info across form screens.
     */
    public static RecordDraft getDraft() {
        if (currentDraft == null) currentDraft = new RecordDraft();
        return currentDraft;
    }

    public static void clearDraft() { currentDraft = null; }

    private Session() {}
}
