package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.catalog.domain.OpportunityTypeRepository;
import hr.fer.artconnect.common.*;
import hr.fer.artconnect.opportunity.domain.*;
import hr.fer.artconnect.opportunity.web.*;
import hr.fer.artconnect.opportunity.domain.OpportunitySearchRepository.OpportunitySearchFilter;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {
    private final OpportunityRepository opportunities;
    private final OpportunityTypeRepository types;
    private final ArtFieldRepository artFields;
    private final OrganizerProfileRepository profiles;
    private final CurrentUserProvider currentUser;
    private final OpportunityMapper mapper;
    private final OpportunityVisibilityPolicy visibilityPolicy;
    private final OpportunityDeletionGuard deletionGuard;
    private final ApplicationCountsProvider applicationCounts;
    private final OpportunitySearchRepository searchRepository;
    private final Clock clock;

    public OpportunityService(OpportunityRepository opportunities, OpportunityTypeRepository types,
            ArtFieldRepository artFields, OrganizerProfileRepository profiles, CurrentUserProvider currentUser,
            OpportunityMapper mapper, OpportunityVisibilityPolicy visibilityPolicy,
            OpportunityDeletionGuard deletionGuard, ApplicationCountsProvider applicationCounts,
            OpportunitySearchRepository searchRepository, @Qualifier("geocodingClock") Clock clock) {
        this.opportunities = opportunities;
        this.types = types;
        this.artFields = artFields;
        this.profiles = profiles;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.visibilityPolicy = visibilityPolicy;
        this.deletionGuard = deletionGuard;
        this.applicationCounts = applicationCounts;
        this.searchRepository = searchRepository;
        // Postojeći UTC Clock dijelimo s geokodiranjem umjesto dodavanja drugog UTC beana.
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizerOpportunity> getMyOpportunities(OrganizerOpportunityQueryParams params) {
        User organizer = requireOrganizer();
        var page = opportunities.findOrganizerOpportunities(organizer.getId(), params.isArchived(),
                params.getStatus(), params.toPageable(Sort.by(
                        Sort.Order.desc("createdAt"), Sort.Order.asc("id"))));
        var profile = profiles.findById(organizer.getId()).orElse(null);
        return PageResponse.from(page.map(opportunity -> mapper.toOrganizerOpportunity(opportunity, profile,
                applicationCounts.getCounts(opportunity.getId()))));
    }

    @Transactional(readOnly = true)
    public OpportunityDto get(UUID opportunityId) {
        Opportunity opportunity = opportunities.findById(opportunityId)
                .orElseThrow(OpportunityService::notFound);
        User viewer = currentUser.getOptionalCurrentUser().orElse(null);
        if (!visibilityPolicy.canView(opportunity, viewer)) {
            throw notFound();
        }
        var profile = profiles.findById(opportunity.getOrganizer().getId()).orElse(null);
        return mapper.toDto(opportunity, profile);
    }

    @Transactional(readOnly = true)
    public PageResponse<OpportunitySummary> search(OpportunitySearchParams params) {
        Set<OpportunityStatus> statuses = params.getStatuses() == null || params.getStatuses().isEmpty()
                ? Set.of(OpportunityStatus.OPEN)
                : params.getStatuses();

        if (!visibilityPolicy.publicStatuses().containsAll(statuses)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Parametri pretrage nisu ispravni.",
                    List.of(new FieldIssue("statuses", "Status nije dopušten u javnoj pretrazi.")));
        }

        if (params.getDeadlineFrom() != null && params.getDeadlineTo() != null
            && params.getDeadlineFrom().isAfter(params.getDeadlineTo())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Parametri pretrage nisu ispravni.", List.of(
                    new FieldIssue("deadlineFrom", "Početak raspona roka ne smije biti nakon završetka.")));
        }
        OffsetDateTime deadlineFrom = params.getDeadlineFrom() == null ? null
                : params.getDeadlineFrom().atStartOfDay(TimeConfiguration.APP_ZONE).toOffsetDateTime();
        OffsetDateTime deadlineTo = params.getDeadlineTo() == null ? null
                : params.getDeadlineTo().plusDays(1).atStartOfDay(TimeConfiguration.APP_ZONE).toOffsetDateTime();

        if (params.getQ() != null) {
            params.setQ(params.getQ().trim());
            if (params.getQ().isEmpty()) params.setQ(null);
        }
        if (params.getCity() != null) {
            params.setCity(params.getCity().trim());
            if (params.getCity().isEmpty()) params.setCity(null);
        }

        Sort order = SortParser.parse(params.getSort(), Set.of("deadline", "publishedAt"));
        Pageable pageable = params.toPageable(order);

        OpportunitySearchFilter filter = new OpportunitySearchFilter(
                params.getQ(), params.getArtFieldIds(), params.getTypeIds(), params.getCity(), params.getLocationType(),
                statuses, deadlineFrom, deadlineTo
        );
        long total = searchRepository.countPublic(filter);
        List<OpportunitySummary> data = searchRepository.findPublic(filter, pageable).stream()
                .map(mapper::toSummary).toList();

        return PageResponse.from(new PageImpl<>(data, pageable, total));
    }

    @Transactional
    public OpportunityDto create(OpportunityInput input) {
        var organizer = requireOrganizer();
        return save(input, null, organizer);
    }

    @Transactional
    public OpportunityDto update(UUID opportunityId, OpportunityInput input) {
        var organizer = requireOrganizer();
        var opportunity = requireOwn(opportunityId, organizer,
                "Možete uređivati samo vlastite prilike.");
        return save(input, opportunity, organizer);
    }

    @Transactional
    public OpportunityDto changeStatus(UUID opportunityId, OpportunityStatusChange input) {
        var organizer = requireOrganizer();
        var opportunity = requireOwn(opportunityId, organizer,
                "Možete mijenjati status samo vlastitih prilika.");

        var target = OpportunityStatus.valueOf(input.status().name());
        var now = OffsetDateTime.now(clock);
        // Do ADM-02-BE svi organizatori smiju objaviti priliku, prema dogovoru u issueu #131.
        OpportunityStatusMachine.validate(opportunity.getStatus(), target, opportunity.isArchived(),
                opportunity.getDeadline(), now, true);
        opportunity.changeStatus(target, now);
        var saved = opportunities.save(opportunity);
        return mapper.toDto(saved, profiles.findById(organizer.getId()).orElse(null));
    }

    @Transactional
    public void delete(UUID opportunityId) {
        User organizer = requireOrganizer();
        Opportunity opportunity = requireOwn(opportunityId, organizer,
                "Možete brisati samo vlastite prilike.");
        deletionGuard.ensureDeletable(opportunity);
        opportunities.delete(opportunity);
    }

    @Transactional
    public OpportunityDto archive(UUID opportunityId) {
        User organizer = requireOrganizer();
        Opportunity opportunity = requireOwn(opportunityId, organizer,
                "Možete arhivirati samo vlastite prilike.");
        opportunity.archive(OffsetDateTime.now(clock));
        return mapper.toDto(opportunity, profiles.findById(organizer.getId()).orElse(null));
    }

    private User requireOrganizer() {
        var organizer = currentUser.getCurrentUser();
        if (organizer.getStatus() == UserStatus.ONBOARDING) {
            throw new ApiException(ErrorCode.ONBOARDING_REQUIRED, "Potrebno je odabrati ulogu.");
        }
        if (organizer.getRole() != UserRole.ORGANIZER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Samo organizator može upravljati prilikama.");
        }
        if (organizer.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }
        if (organizer.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Korisnički račun nije aktivan.");
        }
        return organizer;
    }

    private OpportunityDto save(OpportunityInput input, Opportunity opportunity, User organizer) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (opportunity == null && !input.deadline().isAfter(now)) {
            throw invalid("deadline", "Rok prijave mora biti u budućnosti.");
        }
        if (input.eventEnd() != null
                && (input.eventStart() == null || input.eventEnd().isBefore(input.eventStart()))) {
            throw invalid("eventEnd", "Završetak događanja traži početak i ne smije biti prije njega.");
        }
        Location location = location(input.location());
        if (new HashSet<>(input.artFieldIds()).size() != input.artFieldIds().size()) {
            throw invalid("artFieldIds", "Područja umjetnosti ne smiju se ponavljati.");
        }
        var type = types.findById(input.typeId())
                .filter(item -> item.isActive()
                        || (opportunity != null && item.getId().equals(opportunity.getType().getId())))
                .orElseThrow(() -> invalid("typeId", "Odaberite aktivnu ili već povezanu vrstu prilike."));
        Set<UUID> existingFieldIds = opportunity == null ? Set.of() : opportunity.getArtFields().stream()
                .map(ArtField::getId).collect(Collectors.toSet());
        List<ArtField> selectedFields = artFields.findAllById(input.artFieldIds());
        if (selectedFields.size() != input.artFieldIds().size()
                || selectedFields.stream().anyMatch(item ->
                        !item.isActive() && (opportunity == null || !existingFieldIds.contains(item.getId())))) {
            throw invalid("artFieldIds", "Odaberite aktivna ili već povezana područja umjetnosti.");
        }

        // Sve provjere prethode spremanju. Transakcija sprema priliku i njezina područja zajedno.
        Opportunity saved;
        if (opportunity == null) {
            saved = opportunities.save(Opportunity.create(organizer, type, new LinkedHashSet<>(selectedFields),
                    input.title(), input.description(), input.deadline(), input.criteria(), input.prize(),
                    location, input.eventStart(), input.eventEnd(), now));
        } else {
            opportunity.update(type, new LinkedHashSet<>(selectedFields), input.title(), input.description(),
                    input.deadline(), input.criteria(), input.prize(), location, input.eventStart(), input.eventEnd(), now);
            saved = opportunities.save(opportunity);
        }
        return mapper.toDto(saved, profiles.findById(organizer.getId()).orElse(null));
    }

    private Location location(LocationInput input) {
        if (input.type() == LocationType.ONLINE) {
            if (!isHttpUrl(input.onlineUrl())) {
                throw invalid("location.onlineUrl", "Unesite valjanu HTTP ili HTTPS poveznicu.");
            }
            return Location.online(input.onlineUrl());
        }
        if (input.streetAddress() == null || input.streetAddress().isBlank()) {
            throw invalid("location.streetAddress", "Adresa je obavezna za fizičku lokaciju.");
        }
        if (input.city() == null || input.city().isBlank()) {
            throw invalid("location.city", "Grad je obavezan za fizičku lokaciju.");
        }
        if (input.latitude() == null) {
            throw invalid("location.latitude", "Zemljopisna širina je obavezna za fizičku lokaciju.");
        }
        if (input.longitude() == null) {
            throw invalid("location.longitude", "Zemljopisna dužina je obavezna za fizičku lokaciju.");
        }
        return Location.physical(input.streetAddress(), input.city(), input.postalCode(),
                input.latitude(), input.longitude());
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, "Podaci prilike nisu ispravni.",
                List.of(new FieldIssue(field, message)));
    }

    private Opportunity requireOwn(UUID opportunityId, User organizer, String forbiddenMessage) {
        Opportunity opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Prilika nije pronađena."));
        if (!opportunity.getOrganizer().getId().equals(organizer.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, forbiddenMessage);
        }
        return opportunity;
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Prilika nije pronađena.");
    }
}
