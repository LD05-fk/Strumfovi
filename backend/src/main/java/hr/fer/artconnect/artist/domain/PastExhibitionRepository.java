package hr.fer.artconnect.artist.domain;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PastExhibitionRepository extends JpaRepository<PastExhibition, UUID> {
    List<PastExhibition> findAllByArtistProfile_UserId(UUID artistId);

    Optional<PastExhibition> findByIdAndArtistProfile_UserId(UUID id, UUID artistId);
}
