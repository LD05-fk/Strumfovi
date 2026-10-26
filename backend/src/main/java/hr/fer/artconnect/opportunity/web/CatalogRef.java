package hr.fer.artconnect.opportunity.web;

import java.util.UUID;

public record CatalogRef(
        UUID id, String code, String name
) {}
