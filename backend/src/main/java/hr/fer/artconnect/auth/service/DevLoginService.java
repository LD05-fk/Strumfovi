package hr.fer.artconnect.auth.service;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;

@Service
@Profile("dev & !prod")
public class DevLoginService {
    private final UserRepository userRepository;
    private final OrganizerProfileRepository organizerProfileRepository;

    public DevLoginService(
            UserRepository userRepository,
            OrganizerProfileRepository organizerProfileRepository) {
        this.userRepository = userRepository;
        this.organizerProfileRepository = organizerProfileRepository;
    }

    @Transactional
    public User loginAs(String email) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(candidate -> candidate.getStatus() != UserStatus.DELETED)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND,
                        "Korisnik s navedenom adresom e-pošte nije pronađen."));
        user.loginAsDev();
        return user;
    }

    @Transactional
    public User resetOnboardingUser() {
        User user = userRepository.findByEmailIgnoreCase("novi@artconnect.test")
                .filter(candidate -> candidate.getStatus() != UserStatus.DELETED)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND,
                        "Korisnik novi@artconnect.test nije pronađen."));
        organizerProfileRepository.findById(user.getId())
                .ifPresent(organizerProfileRepository::delete);
        user.resetForDevOnboarding();
        return user;
    }
}
