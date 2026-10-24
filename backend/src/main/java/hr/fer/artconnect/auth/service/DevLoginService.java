package hr.fer.artconnect.auth.service;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;

@Service
@Profile("dev & !prod")
public class DevLoginService {
    private final UserRepository userRepository;

    public DevLoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
