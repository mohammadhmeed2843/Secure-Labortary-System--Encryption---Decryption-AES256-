package javafxapplication7.models;

import javafxapplication7.services.PermissionService;

/**
 * Immutable value object representing a successfully authenticated user.
 * Permission logic is delegated to PermissionService — this class is a pure data carrier.
 */
public class User {

    private final int     userId;
    private final String  username;
    private final Role    role;
    private final String  fullName;
    private final boolean active;

    public User(int userId, String username, Role role, String fullName, boolean active) {
        this.userId   = userId;
        this.username = username;
        this.role     = role;
        this.fullName = fullName;
        this.active   = active;
    }

    /** Convenience constructor for login flow (active is implicitly true). */
    public User(int userId, String username, Role role, String fullName) {
        this(userId, username, role, fullName, true);
    }

    public int     getUserId()  { return userId;   }
    public String  getUsername(){ return username; }
    public Role    getRole()    { return role;     }
    public String  getFullName(){ return fullName; }
    public boolean isActive()   { return active;   }

    public boolean hasRole(Role r)  { return this.role == r; }
    public boolean canUpload()      { return PermissionService.canUpload(role);     }
    public boolean canExport()      { return PermissionService.canExport(role);     }
    public boolean canManageUsers() { return PermissionService.canManageUsers(role); }

    @Override
    public String toString() { return fullName + " [" + role.getDisplayName() + "]"; }
}
