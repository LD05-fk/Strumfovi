package hr.fer.artconnect.opportunity.domain;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.OpportunityType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "opportunities")
public class Opportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private OpportunityType type;

    @ManyToMany
    @JoinTable(name = "opportunity_art_fields",
            joinColumns = @JoinColumn(name = "opportunity_id"),
            inverseJoinColumns = @JoinColumn(name = "art_field_id"))
    private Set<ArtField> artFields = new LinkedHashSet<>();

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private OffsetDateTime deadline;

    @Column(columnDefinition = "text")
    private String criteria;

    @Column(columnDefinition = "text")
    private String prize;

    @Embedded
    private Location location;

    private OffsetDateTime eventStart;

    private OffsetDateTime eventEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OpportunityStatus status = OpportunityStatus.PREPARATION;

    @Column(nullable = false)
    private boolean archived;

    @Column(nullable = false)
    private boolean hiddenByAdmin;

    private OffsetDateTime hiddenAt;

    private OffsetDateTime publishedAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected Opportunity() {}

    // Servis predaje provjerene podatke i vrijeme dobiveno iz ubrizganog Clocka.
    public static Opportunity create(User organizer, OpportunityType type, Set<ArtField> artFields,
            String title, String description, OffsetDateTime deadline, String criteria, String prize,
            Location location, OffsetDateTime eventStart, OffsetDateTime eventEnd, OffsetDateTime now) {
        Opportunity opportunity = new Opportunity();
        opportunity.organizer = organizer;
        opportunity.type = type;
        opportunity.artFields = new LinkedHashSet<>(artFields);
        opportunity.title = title;
        opportunity.description = description;
        opportunity.deadline = deadline;
        opportunity.criteria = criteria;
        opportunity.prize = prize;
        opportunity.location = location;
        opportunity.eventStart = eventStart;
        opportunity.eventEnd = eventEnd;
        opportunity.createdAt = now;
        opportunity.updatedAt = now;
        return opportunity;
    }

    public UUID getId() {
        return id;
    }

    public User getOrganizer() {
        return organizer;
    }

    public OpportunityType getType() {
        return type;
    }

    public Set<ArtField> getArtFields() {
        return Collections.unmodifiableSet(artFields);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getDeadline() {
        return deadline;
    }

    public String getCriteria() {
        return criteria;
    }

    public String getPrize() {
        return prize;
    }

    public Location getLocation() {
        return location;
    }

    public OffsetDateTime getEventStart() {
        return eventStart;
    }

    public OffsetDateTime getEventEnd() {
        return eventEnd;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isHiddenByAdmin() {
        return hiddenByAdmin;
    }

    public OffsetDateTime getHiddenAt() {
        return hiddenAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
