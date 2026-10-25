package hr.fer.artconnect.artist.domain;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistLinkRepository extends JpaRepository<ArtistLink, UUID> {
    @Query("""
            select link from ArtistLink link
            where link.artistProfile.userId = :userId
            order by link.position, link.id
            """)
    List<ArtistLink> findAllForArtistProfile(@Param("userId") UUID userId);
}
