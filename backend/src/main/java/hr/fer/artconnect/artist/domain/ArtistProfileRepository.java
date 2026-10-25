package hr.fer.artconnect.artist.domain;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, UUID> {
    @Override
    @EntityGraph(attributePaths = "artFields")
    Optional<ArtistProfile> findById(UUID id);
}
