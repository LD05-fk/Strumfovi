package hr.fer.artconnect.auth.web;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import java.util.UUID;

public record OrganizerProfileDto(
        UUID userId,
        String displayName,
        String organizationName,
        OrganizerApproval organizerApproval) {
}
