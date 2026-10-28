package hr.fer.artconnect.opportunity.web;

import java.util.UUID;

public record OrganizerSummary(
        UUID id, String displayName, String organizationName
) {}
