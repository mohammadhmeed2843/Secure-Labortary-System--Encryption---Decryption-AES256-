package javafxapplication7.model;

import java.time.LocalDateTime;

/** Represents a row from the audit_log table. */
public class AuditEntry {

    private int           logId;
    private Integer       userId;        // null if the user was deleted
    private String        username;
    private String        action;
    private String        targetType;    // 'file', 'user', 'session', 'system'
    private String        targetId;
    private String        details;
    private LocalDateTime createdAt;

    public AuditEntry() {}

    public int           getLogId()      { return logId;      }
    public void          setLogId(int v) { this.logId = v;    }
    public Integer       getUserId()     { return userId;     }
    public void          setUserId(Integer v) { this.userId = v; }
    public String        getUsername()   { return username;   }
    public void          setUsername(String v) { this.username = v; }
    public String        getAction()     { return action;     }
    public void          setAction(String v) { this.action = v; }
    public String        getTargetType() { return targetType; }
    public void          setTargetType(String v) { this.targetType = v; }
    public String        getTargetId()   { return targetId;   }
    public void          setTargetId(String v) { this.targetId = v; }
    public String        getDetails()    { return details;    }
    public void          setDetails(String v) { this.details = v; }
    public LocalDateTime getCreatedAt()  { return createdAt;  }
    public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
