package hr.fer.artconnect.catalog.web;

import java.util.UUID;

public record CatalogItem(UUID id, String code, String name, boolean active, short position) {
}
