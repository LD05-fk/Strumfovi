package hr.fer.artconnect.auth.service;

import hr.fer.artconnect.auth.domain.OrganizerApproval;

public final class OrganizerApprovalPolicy {
    private OrganizerApprovalPolicy() {}

    public static OrganizerApproval defaultForNewOrganizer() {
        return OrganizerApproval.APPROVED;
    }
}
