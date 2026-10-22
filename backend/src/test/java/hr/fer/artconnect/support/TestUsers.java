package hr.fer.artconnect.support;

import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;

// Stvarni entitet za jedinične testove bez baze, kako bi testovi provjerili što metode entiteta mijenjaju.
public final class TestUsers {

    private TestUsers() {
    }

    public static User user(UUID id, String googleSub, String email, UserRole role, UserStatus status) {
        User user = User.createOnboardingUser(googleSub, email, "Test korisnik", null);
        // Polja koja inače postavljaju baza ili drugi tokovi (npr. unaprijed stvoreni administrator).
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "status", status);
        ReflectionTestUtils.setField(user, "lastLoginAt", null);
        return user;
    }
}
