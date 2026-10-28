package hr.fer.artconnect.opportunity.web;

import java.math.BigDecimal;
import hr.fer.artconnect.opportunity.domain.LocationType;

public record LocationDto(
        LocationType type, String streetAddress, String city, String postalCode,
        BigDecimal latitude, BigDecimal longitude, String onlineUrl
) {}
