package hr.fer.artconnect.opportunity.service.geocoding;

import java.time.Duration;
import java.net.URI;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("artconnect.geocoding")
@Validated
public class GeocodingProperties {
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String userAgent = "ArtConnect/1.0 (+https://artconnect-fer.duckdns.org)";
    @NotNull
    private Duration minInterval = Duration.ofSeconds(1);
    @NotNull
    private Duration maxWait = Duration.ofSeconds(2);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Duration getMinInterval() {
        return minInterval;
    }

    public void setMinInterval(Duration minInterval) {
        this.minInterval = minInterval;
    }

    public Duration getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Duration maxWait) {
        this.maxWait = maxWait;
    }

    @PostConstruct
    void validate() {
        URI uri = URI.create(baseUrl);
        if (!uri.isAbsolute() || uri.getHost() == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("artconnect.geocoding.base-url must be an absolute HTTP(S) URL.");
        }
        if (minInterval.isNegative() || maxWait.isNegative()) {
            throw new IllegalArgumentException("Geocoding rate-limit durations must be non-negative.");
        }
    }
}
