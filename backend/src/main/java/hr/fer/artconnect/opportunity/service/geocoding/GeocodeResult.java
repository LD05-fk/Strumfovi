package hr.fer.artconnect.opportunity.service.geocoding;

import java.math.BigDecimal;

public record GeocodeResult(
        String displayName,
        BigDecimal latitude,
        BigDecimal longitude,
        String city,
        String postalCode) {}
