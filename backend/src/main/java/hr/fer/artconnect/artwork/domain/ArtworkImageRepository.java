package hr.fer.artconnect.artwork.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkImageRepository extends JpaRepository<ArtworkImage, UUID> {
    @Query("""
            SELECT image FROM ArtworkImage image
            JOIN FETCH image.file
            WHERE image.artwork.id IN :artworkIds
            ORDER BY image.artwork.id, image.position
            """)
    List<ArtworkImage> findAllWithFileByArtworkIds(@Param("artworkIds") Collection<UUID> artworkIds);

    @Query("""
            SELECT image FROM ArtworkImage image
            JOIN FETCH image.artwork artwork
            JOIN FETCH artwork.artist
            WHERE image.file.id = :fileId
            """)
    Optional<ArtworkImage> findWithArtworkByFileId(@Param("fileId") UUID fileId);
}
