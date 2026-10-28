package hr.fer.artconnect.opportunity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Lokacija se sprema u stupce tablice opportunities, bez zasebne tablice. */
@Embeddable
public class Location {
    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 16)
    private LocationType type;

    @Column(length = 200)
    private String streetAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String postalCode;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(length = 1024)
    private String onlineUrl;

    protected Location() {}

    // Pozivatelj prije stvaranja provjerava obavezna polja i raspon koordinata.
    public static Location physical(String streetAddress, String city, String postalCode,
            BigDecimal latitude, BigDecimal longitude) {
        Location location = new Location();
        location.type = LocationType.PHYSICAL;
        location.streetAddress = streetAddress;
        location.city = city;
        location.postalCode = postalCode;
        location.latitude = latitude.setScale(6, RoundingMode.HALF_UP);
        location.longitude = longitude.setScale(6, RoundingMode.HALF_UP);
        return location;
    }

    public static Location online(String onlineUrl) {
        Location location = new Location();
        location.type = LocationType.ONLINE;
        location.onlineUrl = onlineUrl;
        return location;
    }

    public LocationType getType() {
        return type;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getOnlineUrl() {
        return onlineUrl;
    }
}
