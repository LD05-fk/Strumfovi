package hr.fer.artconnect.catalog.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

// Oba šifrarnika imaju ista polja, ali ostaju u odvojenim tablicama.
@MappedSuperclass
public abstract class CatalogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String code;
    private String name;
    private boolean active;
    private short position;

    public void update(String code, String name, boolean active, short position) {
        this.code = code;
        this.name = name;
        this.active = active;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public short getPosition() {
        return position;
    }
}
