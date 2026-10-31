package hr.fer.artconnect.artwork.service;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class ArtworkVisibilityPolicy {
    public boolean canViewArtist(User artist, User viewer) {
        if (artist.getRole() != UserRole.ARTIST) {
            return false;
        }
        return isOwnerOrAdministrator(artist, viewer) || artist.getStatus() == UserStatus.ACTIVE;
    }

    public boolean canViewArtwork(Artwork artwork, User viewer) {
        User artist = artwork.getArtist();
        if (!canViewArtist(artist, viewer)) {
            return false;
        }
        return isOwnerOrAdministrator(artist, viewer)
                || (artwork.getVisibility() == ArtworkVisibility.PUBLIC && !artwork.isHiddenByAdmin());
    }

    public boolean canViewAllArtworks(User artist, User viewer) {
        return isOwnerOrAdministrator(artist, viewer);
    }

    private static boolean isOwnerOrAdministrator(User artist, User viewer) {
        return viewer != null && (artist.getId().equals(viewer.getId())
                || viewer.getRole() == UserRole.ADMIN);
    }
}
