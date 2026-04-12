package javafxapplication7.session;

import javafxapplication7.model.RecordDraft;
import javafxapplication7.model.Role;
import javafxapplication7.model.User;

/**
 * Application-wide session state.
 * Holds the logged-in user and the current upload draft.
 * Replaces CryptoStore — no cryptographic material is stored here.
 */
public final class Session {

    private static User        currentUser;
    private static RecordDraft currentDraft;

    public static void login(User user) {
        currentUser  = user;
        currentDraft = null;
    }

    public static void logout() {
        currentUser  = null;
        currentDraft = null;
    }

    public static User    getUser()      { return currentUser; }
    public static boolean isLoggedIn()   { return currentUser != null; }

    public static boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    /** True if the current user is allowed to upload/encrypt files. */
    public static boolean canUpload() {
        return currentUser != null && currentUser.canUpload();
    }

    /** True if the current user is allowed to export/decrypt files. */
    public static boolean canExport() {
        return currentUser != null && currentUser.canExport();
    }

    // ── Upload draft ─────────────────────────────────────────────────────────

    /**
     * Returns the active draft, creating a fresh one if none exists.
     * The draft accumulates the file + patient + test info across screens.
     */
    public static RecordDraft getDraft() {
        if (currentDraft == null) currentDraft = new RecordDraft();
        return currentDraft;
    }

    public static void clearDraft() { currentDraft = null; }

    private Session() {}
}
