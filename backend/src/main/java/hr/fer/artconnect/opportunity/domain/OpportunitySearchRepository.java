package hr.fer.artconnect.opportunity.domain;

import hr.fer.artconnect.auth.domain.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class OpportunitySearchRepository {
    private final JdbcClient db;

    public OpportunitySearchRepository(JdbcClient db) {
        this.db = db;
    }

    public long countPublic(OpportunitySearchFilter filter) {
        Conditions conditions = getConditions(filter);

        String sql = "SELECT COUNT(*) AS total " + getOpportunitiesFrom()
                + " WHERE " + String.join(" AND ", conditions.where());

        return db.sql(sql).params(conditions.params()).query(Long.class).single();
    }

    public List<OpportunityRow> findPublic(OpportunitySearchFilter filter, Pageable pageable) {
        Conditions conditions = getConditions(filter);

        String sql = getOpportunitiesSelect() + getOpportunitiesFrom()
                + " WHERE " + String.join(" AND ", conditions.where())
                + " ORDER BY " + orderBy(pageable.getSort())
                + " LIMIT :limit OFFSET :offset";
        conditions.params().put("limit", pageable.getPageSize());
        conditions.params().put("offset", pageable.getOffset());

        List<OpportunityRow> opportunities = db.sql(sql).params(conditions.params()).query(this::mapRow).list();
        return this.withArtFields(opportunities);
    }

    private static String getOpportunitiesSelect() {
        return """
            SELECT
                o.id AS id,
                o.title AS title,
                ot.id AS type_id,
                ot.code AS type_code,
                ot.name AS type_name,
                o.deadline AS deadline,
                o.status AS status,
                o.location_type AS location_type,
                o.city AS city,
                o.latitude AS latitude,
                o.longitude AS longitude,
                o.event_start AS event_start,
                u.id AS organizer_id,
                u.display_name AS organizer_display_name,
                u.status AS organizer_status,
                op.organization_name AS organization_name
            """;
    }

    private static String getOpportunitiesFrom() {
        return """
            FROM opportunities o
            JOIN users u ON u.id = o.organizer_id
            JOIN opportunity_types ot ON ot.id = o.type_id
            LEFT JOIN organizer_profiles op ON op.user_id = u.id
            """;
    }

    private static String getArtFieldsQuery() {
        return """
            SELECT oaf.opportunity_id AS opportunity_id,
                   a.id AS art_field_id,
                   a.code AS code,
                   a.name AS name
            FROM opportunity_art_fields oaf
            JOIN art_fields a ON a.id = oaf.art_field_id
            WHERE oaf.opportunity_id IN (:ids)
            ORDER BY a.position, a.code
            """;
    }

    private static String orderBy(Sort sort) {
        Sort.Order order = sort.stream().findFirst().orElse(null);
        if (order == null) return "o.deadline ASC, o.id";
        String column = switch (order.getProperty()) {
            case "deadline" -> "o.deadline";
            case "publishedAt" -> "o.published_at";
            default -> throw new IllegalArgumentException("Nepodržano sortiranje: " + order.getProperty());
        };
        return column + (order.isDescending() ? " DESC" : " ASC") + ", o.id";
    }

    private OpportunityRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OpportunityRow(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                new CatalogRow(
                        rs.getObject("type_id", UUID.class),
                        rs.getString("type_code"),
                        rs.getString("type_name")),
                List.of(),
                rs.getObject("deadline", OffsetDateTime.class),
                OpportunityStatus.valueOf(rs.getString("status")),
                LocationType.valueOf(rs.getString("location_type")),
                rs.getString("city"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getObject("event_start", OffsetDateTime.class),
                new OrganizerRow(
                        rs.getObject("organizer_id", UUID.class),
                        rs.getString("organizer_display_name"),
                        UserStatus.valueOf(rs.getString("organizer_status")),
                        rs.getString("organization_name"))
        );
    }

    private List<OpportunityRow> withArtFields(List<OpportunityRow> rows) {
        if (rows.isEmpty()) return rows;
        List<UUID> ids = rows.stream().map(OpportunityRow::id).toList();
        Map<UUID, List<CatalogRow>> byOpportunity = new HashMap<>();
        db.sql(getArtFieldsQuery()).param("ids", ids).query(rs -> {
           CatalogRow artField = new CatalogRow(
                   rs.getObject("art_field_id", UUID.class),
                   rs.getString("code"),
                   rs.getString("name"));
           UUID opportunityId = rs.getObject("opportunity_id", UUID.class);
           if (!byOpportunity.containsKey(opportunityId)) {
               byOpportunity.put(opportunityId, new ArrayList<>());
           }
           byOpportunity.get(opportunityId).add(artField);
        });
        return rows.stream().map(
                row -> row.withArtFields(byOpportunity.getOrDefault(row.id(), List.of()))).toList();
    }

    private Conditions getConditions(OpportunitySearchFilter filter) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        where.add("o.archived = false");
        where.add("o.hidden_by_admin = false");
        where.add("u.status <> 'BLOCKED'");

        // Zadani status ['OPEN'] postavlja servis
        where.add("o.status IN (:statuses)");
        params.put("statuses", filter.statuses().stream().map(Enum::name).toList());

        if (filter.query() != null && !filter.query().isBlank()) {
            where.add("to_tsvector('simple', o.title || ' ' || o.description) @@ plainto_tsquery('simple', :q)");
            params.put("q", filter.query());
        }

        if (filter.artFieldIds() != null && !filter.artFieldIds().isEmpty()) {
            where.add("""
                EXISTS (SELECT 1 FROM opportunity_art_fields f
                WHERE f.opportunity_id = o.id AND f.art_field_id IN (:artFieldIds))""");
            params.put("artFieldIds", filter.artFieldIds());
        }
        if (filter.typeIds() != null && !filter.typeIds().isEmpty()) {
            where.add("o.type_id IN (:typeIds)");
            params.put("typeIds", filter.typeIds());
        }
        if (filter.city() != null && !filter.city().isBlank()) {
            where.add("lower(o.city) = lower(:city)");
            params.put("city", filter.city());
        }
        if (filter.locationType() != null) {
            where.add("o.location_type = :locationType");
            params.put("locationType", filter.locationType().name());
        }
        if (filter.deadlineFrom() != null) {
            where.add("o.deadline >= :deadlineFrom");
            params.put("deadlineFrom", filter.deadlineFrom());
        }
        if (filter.deadlineTo() != null) {
            where.add("o.deadline < :deadlineTo");
            params.put("deadlineTo", filter.deadlineTo());
        }

        return new Conditions(where, params);
    }

    private record Conditions(List<String> where, Map<String, Object> params) {}

    public record OpportunityRow(
            UUID id,
            String title,
            CatalogRow type,
            List<CatalogRow> artFields,
            OffsetDateTime deadline,
            OpportunityStatus status,
            LocationType locationType,
            String city,
            BigDecimal latitude,
            BigDecimal longitude,
            OffsetDateTime eventStart,
            OrganizerRow organizer) {
        public OpportunityRow withArtFields(List<CatalogRow> fields) {
            return new OpportunityRow(id, title, type, fields, deadline, status, locationType,
                    city, latitude, longitude, eventStart, organizer);
        }
    }

    public record CatalogRow(UUID id, String code, String name) {}

    public record OrganizerRow(UUID id, String displayName, UserStatus status, String organizationName) {}

    public record OpportunitySearchFilter(
            String query,
            List<UUID> artFieldIds,
            List<UUID> typeIds,
            String city,
            LocationType locationType,
            Set<OpportunityStatus> statuses,
            OffsetDateTime deadlineFrom,
            OffsetDateTime deadlineTo
    ) {}
}
