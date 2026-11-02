package hr.fer.artconnect.artwork.domain;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkRepository extends JpaRepository<Artwork, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT artwork FROM Artwork artwork WHERE artwork.id = :artworkId")
    Optional<Artwork> findByIdForUpdate(@Param("artworkId") UUID artworkId);

    Page<Artwork> findAllByArtist_Id(UUID artistId, Pageable pageable);

    Page<Artwork> findAllByArtist_IdAndVisibilityAndHiddenByAdminFalse(
            UUID artistId, ArtworkVisibility visibility, Pageable pageable);
}
