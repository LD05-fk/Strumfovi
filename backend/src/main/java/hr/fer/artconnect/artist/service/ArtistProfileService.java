package hr.fer.artconnect.artist.service;

import hr.fer.artconnect.artist.domain.ArtistLinkRepository;
import hr.fer.artconnect.artist.domain.ArtistLink;
import hr.fer.artconnect.artist.domain.ArtistProfile;
import hr.fer.artconnect.artist.domain.ArtistProfileRepository;
import hr.fer.artconnect.artist.web.ArtistProfileDto;
import hr.fer.artconnect.artist.web.ArtistProfileUpdateRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.common.FieldIssue;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistProfileService {
    private final ArtistProfileRepository artistProfiles;
    private final ArtistLinkRepository artistLinks;
    private final ArtFieldRepository artFields;
    private final CurrentUserProvider currentUserProvider;

    public ArtistProfileService(ArtistProfileRepository artistProfiles, ArtistLinkRepository artistLinks,
            ArtFieldRepository artFields, CurrentUserProvider currentUserProvider) {
        this.artistProfiles = artistProfiles;
        this.artistLinks = artistLinks;
        this.artFields = artFields;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ArtistProfileDto getMyArtistProfile() {
        var user = currentUserProvider.getCurrentUser();
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }

        var profile = artistProfiles.findById(user.getId()).orElse(null);
        List<ArtistLink> links = profile == null ? List.of()
                : artistLinks.findAllForArtistProfile(user.getId());
        return ArtistProfileMapper.toDto(user, profile, links);
    }

    @Transactional
    public ArtistProfileDto updateMyArtistProfile(ArtistProfileUpdateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }

        List<FieldIssue> issues = new ArrayList<>();
        Set<UUID> submittedIds = new HashSet<>(request.artFieldIds());
        if (submittedIds.size() != request.artFieldIds().size()) {
            issues.add(new FieldIssue("artFieldIds", "Područja umjetnosti ne smiju se ponavljati."));
        }
        if (!issues.isEmpty()) {
            throw validationError(issues);
        }

        ArtistProfile profile = artistProfiles.findById(user.getId())
                .orElseGet(() -> artistProfiles.save(ArtistProfile.forUser(user)));
        Set<UUID> previouslyLinkedIds = profile.getArtFields().stream()
                .map(ArtField::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<ArtField> requestedFields = artFields.findAllById(request.artFieldIds());
        Set<UUID> foundIds = requestedFields.stream().map(ArtField::getId).collect(java.util.stream.Collectors.toSet());

        for (UUID id : request.artFieldIds()) {
            ArtField field = requestedFields.stream().filter(candidate -> candidate.getId().equals(id))
                    .findFirst().orElse(null);
            if (field == null || (!field.isActive() && !previouslyLinkedIds.contains(id))) {
                issues.add(new FieldIssue("artFieldIds", "Područje umjetnosti ne postoji ili nije aktivno."));
                break;
            }
        }
        if (foundIds.size() != request.artFieldIds().size() && issues.isEmpty()) {
            issues.add(new FieldIssue("artFieldIds", "Područje umjetnosti ne postoji ili nije aktivno."));
        }

        for (int index = 0; index < request.links().size(); index++) {
            if (!isValidUrl(request.links().get(index).url())) {
                issues.add(new FieldIssue("links[" + index + "].url",
                        "URL mora biti valjana HTTP ili HTTPS adresa s nazivom hosta."));
            }
        }
        if (!issues.isEmpty()) {
            throw validationError(issues);
        }

        profile.replace(request.biography(), request.artistStatement(), requestedFields);
        List<ArtistLink> previousLinks = artistLinks.findAllForArtistProfile(user.getId());
        artistLinks.deleteAll(previousLinks);
        List<ArtistLink> replacements = new ArrayList<>(request.links().size());
        for (int index = 0; index < request.links().size(); index++) {
            var link = request.links().get(index);
            String label = link.label() == null || link.label().isBlank() ? null : link.label();
            replacements.add(ArtistLink.create(profile, label, link.url(), (short) index));
        }
        List<ArtistLink> savedLinks = artistLinks.saveAll(replacements);
        return ArtistProfileMapper.toDto(user, profile, savedLinks);
    }

    private static boolean isValidUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static ApiException validationError(List<FieldIssue> issues) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, "Neki podaci nisu ispravni.", issues);
    }
}
