package hr.fer.artconnect.artwork.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkImageRepository extends JpaRepository<ArtworkImage, UUID> {
    long countByArtwork_Id(UUID artworkId);

    @Query("SELECT MAX(image.position) FROM ArtworkImage image WHERE image.artwork.id = :artworkId")
    Short findMaxPositionByArtworkId(@Param("artworkId") UUID artworkId);

    @Query("""
            SELECT image FROM ArtworkImage image
            JOIN FETCH image.file
            WHERE image.artwork.id IN :artworkIds
            ORDER BY image.artwork.id, image.position
            """)
    List<ArtworkImage> findAllWithFileByArtworkIds(@Param("artworkIds") Collection<UUID> artworkIds);
}
