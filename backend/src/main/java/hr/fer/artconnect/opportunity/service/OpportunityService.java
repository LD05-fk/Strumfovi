package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.catalog.domain.OpportunityTypeRepository;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.common.FieldIssue;
import hr.fer.artconnect.opportunity.domain.Location;
import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.domain.OpportunityRepository;
import hr.fer.artconnect.opportunity.web.LocationInput;
import hr.fer.artconnect.opportunity.web.OpportunityDto;
import hr.fer.artconnect.opportunity.web.OpportunityInput;
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
    private final Clock clock;

    public OpportunityService(OpportunityRepository opportunities, OpportunityTypeRepository types,
            ArtFieldRepository artFields, OrganizerProfileRepository profiles, CurrentUserProvider currentUser,
            OpportunityMapper mapper, @Qualifier("geocodingClock") Clock clock) {
        this.opportunities = opportunities;
        this.types = types;
        this.artFields = artFields;
        this.profiles = profiles;
        this.currentUser = currentUser;
        this.mapper = mapper;
        // Postojeći UTC Clock dijelimo s geokodiranjem umjesto dodavanja drugog UTC beana.
        this.clock = clock;
    }

    @Transactional
    public OpportunityDto create(OpportunityInput input) {
        var organizer = requireOrganizer();
        return save(input, null, organizer);
    }

    @Transactional
    public OpportunityDto update(UUID opportunityId, OpportunityInput input) {
        var organizer = requireOrganizer();
        var opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Prilika nije pronađena."));
        if (!opportunity.getOrganizer().getId().equals(organizer.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Možete uređivati samo vlastite prilike.");
        }
        return save(input, opportunity, organizer);
    }

    private User requireOrganizer() {
        var organizer = currentUser.getCurrentUser();
        if (organizer.getStatus() == UserStatus.ONBOARDING) {
            throw new ApiException(ErrorCode.ONBOARDING_REQUIRED, "Potrebno je odabrati ulogu.");
        }
        if (organizer.getRole() != UserRole.ORGANIZER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Samo organizator može stvarati i uređivati prilike.");
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
}
