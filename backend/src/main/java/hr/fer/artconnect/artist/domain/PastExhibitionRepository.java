package hr.fer.artconnect.artist.domain;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PastExhibitionRepository extends JpaRepository<PastExhibition, UUID> {
    List<PastExhibition> findAllByArtistProfile_UserId(UUID artistId);
}
