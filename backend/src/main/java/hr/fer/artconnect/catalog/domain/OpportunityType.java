package hr.fer.artconnect.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "opportunity_types")
public class OpportunityType extends CatalogEntry {
    public OpportunityType() {}
}
