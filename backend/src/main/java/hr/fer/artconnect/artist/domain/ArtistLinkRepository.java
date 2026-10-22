package hr.fer.artconnect.artist.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistLinkRepository extends JpaRepository<ArtistLink, UUID> {
}
