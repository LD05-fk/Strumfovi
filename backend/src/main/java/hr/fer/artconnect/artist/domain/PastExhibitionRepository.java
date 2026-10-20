package hr.fer.artconnect.artist.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PastExhibitionRepository extends JpaRepository<PastExhibition, UUID> {
}
