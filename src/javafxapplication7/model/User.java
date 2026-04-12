package javafxapplication7.model;

public class User {

    private final int    userId;
    private final String username;
    private final Role   role;
    private final String fullName;

    public User(int userId, String username, Role role, String fullName) {
        this.userId   = userId;
        this.username = username;
        this.role     = role;
        this.fullName = fullName;
    }

    public int    getUserId()  { return userId;   }
    public String getUsername(){ return username; }
    public Role   getRole()    { return role;     }
    public String getFullName(){ return fullName; }

    public boolean hasRole(Role r) { return this.role == r; }
    public boolean canUpload()     { return role == Role.ADMIN || role == Role.TECHNICIAN; }
    public boolean canExport()     { return role == Role.ADMIN || role == Role.DOCTOR;     }

    @Override
    public String toString() { return fullName + " [" + role.getDisplayName() + "]"; }
}
