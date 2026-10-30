package hr.fer.artconnect.auth.web;

import hr.fer.artconnect.auth.service.OrganizerProfileService;
import hr.fer.artconnect.common.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/me/organizer-profile")
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerProfileController {
    private final OrganizerProfileService organizerProfiles;

    public OrganizerProfileController(OrganizerProfileService organizerProfiles) {
        this.organizerProfiles = organizerProfiles;
    }

    @GetMapping
    public OrganizerProfileDto getMyOrganizerProfile() {
        return organizerProfiles.getMyOrganizerProfile();
    }

    @PutMapping
    public OrganizerProfileDto updateMyOrganizerProfile(
            @Valid @RequestBody OrganizerProfileUpdateRequest request) {
        return organizerProfiles.updateMyOrganizerProfile(request);
    }
}
