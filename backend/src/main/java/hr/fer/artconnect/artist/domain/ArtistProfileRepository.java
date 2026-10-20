package hr.fer.artconnect.artist.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, UUID> {
}
