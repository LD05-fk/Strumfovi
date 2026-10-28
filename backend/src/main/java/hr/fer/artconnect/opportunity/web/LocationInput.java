package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.opportunity.domain.LocationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Servis dodatno provjerava obavezna polja odabrane vrste i HTTP/HTTPS poveznicu. */
public record LocationInput(
        @NotNull(message = "Vrsta lokacije je obavezna.")
        LocationType type,
        @Size(max = 200, message = "Adresa smije imati najviše 200 znakova.")
        String streetAddress,
        @Size(max = 100, message = "Grad smije imati najviše 100 znakova.")
        String city,
        @Size(max = 10, message = "Poštanski broj smije imati najviše 10 znakova.")
        String postalCode,
        @DecimalMin(value = "-90", message = "Zemljopisna širina mora biti između -90 i 90.")
        @DecimalMax(value = "90", message = "Zemljopisna širina mora biti između -90 i 90.")
        BigDecimal latitude,
        @DecimalMin(value = "-180", message = "Zemljopisna dužina mora biti između -180 i 180.")
        @DecimalMax(value = "180", message = "Zemljopisna dužina mora biti između -180 i 180.")
        BigDecimal longitude,
        @Size(max = 1024, message = "Poveznica smije imati najviše 1024 znaka.")
        String onlineUrl
) {
    public LocationInput {
        streetAddress = strip(streetAddress);
        city = strip(city);
        postalCode = strip(postalCode);
        onlineUrl = strip(onlineUrl);
        // Polja koja ne pripadaju odabranoj vrsti lokacije ignoriraju se.
        if (type == LocationType.ONLINE) {
            streetAddress = null;
            city = null;
            postalCode = null;
            latitude = null;
            longitude = null;
        } else if (type == LocationType.PHYSICAL) {
            onlineUrl = null;
        }
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
