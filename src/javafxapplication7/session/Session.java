package javafxapplication7.session;

import javafxapplication7.models.RecordDraft;
import javafxapplication7.models.Role;
import javafxapplication7.models.User;
import javafxapplication7.services.PermissionService;

/**
 * Application-wide session state.
 * Holds the logged-in user and the active upload draft.
 * Both fields are volatile so changes on the FX thread are visible to background Tasks.
 */
public final class Session {

    private static volatile User        currentUser;
    private static volatile RecordDraft currentDraft;

    public static void login(User user) {
        currentUser  = user;
        currentDraft = null;
    }

    public static void logout() {
        currentUser  = null;
        currentDraft = null;
    }

    public static User    getUser()    { return currentUser;         }
    public static boolean isLoggedIn() { return currentUser != null; }

    public static boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

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

    public static RecordDraft getDraft() {
        if (currentDraft == null) currentDraft = new RecordDraft();
        return currentDraft;
    }

    public static void clearDraft() { currentDraft = null; }

    private Session() {}
}
