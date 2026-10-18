package hr.fer.artconnect.auth;

import java.io.*;
import java.security.*;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.*;

// Principal za SecurityContext i JDBC sesiju; nije DTO odgovora GET /me.
public record CurrentUser(UUID id, Role role, Status status) implements Principal, Serializable {
    public CurrentUser {
        Objects.requireNonNull(id);
        Objects.requireNonNull(status);
    }

    @Override
    public String getName() {
        // Stabilno ime omogućuje dohvat i poništavanje svih sesija po ID-u korisnika.
        return id.toString();
    }

    public List<GrantedAuthority> getAuthorities() {
        // Korisnik tijekom onboardinga još nema odabranu ulogu.
        return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public enum Role {
        ARTIST, ORGANIZER, ADMIN
    }

    public enum Status {
        INVITED, ONBOARDING, ACTIVE, BLOCKED, DELETED
    }
}
