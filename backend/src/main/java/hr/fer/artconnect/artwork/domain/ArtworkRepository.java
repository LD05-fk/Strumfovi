package hr.fer.artconnect.artwork.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtworkRepository extends JpaRepository<Artwork, UUID> {
    Page<Artwork> findAllByArtist_Id(UUID artistId, Pageable pageable);

    Page<Artwork> findAllByArtist_IdAndVisibilityAndHiddenByAdminFalse(
            UUID artistId, ArtworkVisibility visibility, Pageable pageable);
}
