package hr.fer.artconnect.admin.web;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.PageRequestParams;
import jakarta.validation.constraints.Size;

public class AdminUserQueryParams extends PageRequestParams {
    @Size(max = 100, message = "{validation.admin.users.q.max}")
    private String q;

    private UserRole role;
    private UserStatus status;
    private OrganizerApproval organizerApproval;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public OrganizerApproval getOrganizerApproval() {
        return organizerApproval;
    }

    public void setOrganizerApproval(OrganizerApproval organizerApproval) {
        this.organizerApproval = organizerApproval;
    }
}
