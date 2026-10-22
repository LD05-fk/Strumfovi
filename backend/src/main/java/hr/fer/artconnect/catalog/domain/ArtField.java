package hr.fer.artconnect.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "art_fields")
public class ArtField extends CatalogEntry {
    public ArtField() {}
}
