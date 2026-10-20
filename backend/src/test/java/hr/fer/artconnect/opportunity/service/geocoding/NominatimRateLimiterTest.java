package hr.fer.artconnect.opportunity.service.geocoding;

import static org.assertj.core.api.Assertions.*;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NominatimRateLimiterTest {
    @Test
    void odvajaDvaPozivaNajmanjeJednomSekundom() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-19T08:00:00Z"));
        GeocodingProperties properties = properties(Duration.ofSeconds(1), Duration.ofSeconds(2));
        NominatimRateLimiter limiter = new NominatimRateLimiter(properties, clock, clock::advance);

        Instant firstPermit = limiter.execute(clock::instant);
        Instant secondPermit = limiter.execute(clock::instant);

        assertThat(Duration.between(firstPermit, secondPermit)).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void vraca429KadTerminNijeDostupanUnutarMaxWait() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-19T08:00:00Z"));
        GeocodingProperties properties = properties(Duration.ofSeconds(3), Duration.ofSeconds(2));
        NominatimRateLimiter limiter = new NominatimRateLimiter(properties, clock, clock::advance);
        limiter.execute(() -> null);

        assertThatThrownBy(() -> limiter.execute(() -> null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.RATE_LIMITED);
    }

    @Test
    void konkurentniPoziviKoristeIstiGlobalniRazmak() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-19T08:00:00Z"));
        GeocodingProperties properties = properties(Duration.ofSeconds(1), Duration.ofSeconds(2));
        NominatimRateLimiter limiter = new NominatimRateLimiter(properties, clock, clock::advance);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> limiter.execute(clock::instant));
            var second = executor.submit(() -> limiter.execute(clock::instant));
            Instant firstPermit = first.get();
            Instant secondPermit = second.get();

            assertThat(Duration.between(firstPermit, secondPermit).abs())
                    .isGreaterThanOrEqualTo(Duration.ofSeconds(1));
        } finally {
            executor.shutdownNow();
        }
    }

    private static GeocodingProperties properties(Duration minInterval, Duration maxWait) {
        GeocodingProperties properties = new GeocodingProperties();
        properties.setMinInterval(minInterval);
        properties.setMaxWait(maxWait);
        return properties;
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }

        private void advance(Duration duration) {
            instant.updateAndGet(value -> value.plus(duration));
        }
    }
}
