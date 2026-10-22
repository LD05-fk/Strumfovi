package hr.fer.artconnect.opportunity.service.geocoding;

import static org.assertj.core.api.Assertions.*;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
        AtomicReference<Duration> scheduledWait = new AtomicReference<>(Duration.ZERO);
        NominatimRateLimiter limiter = new NominatimRateLimiter(properties, clock, duration -> {
            scheduledWait.set(duration);
            clock.advance(duration);
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> limiter.execute(() -> null));
            var second = executor.submit(() -> limiter.execute(() -> null));
            first.get();
            second.get();

            assertThat(scheduledWait.get()).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sporVanjskiPozivNeBlokiraRezervacijuSljedecegTermina() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-19T08:00:00Z"));
        GeocodingProperties properties = properties(Duration.ZERO, Duration.ofMillis(200));
        NominatimRateLimiter limiter = new NominatimRateLimiter(properties, clock, clock::advance);
        CountDownLatch outboundStarted = new CountDownLatch(1);
        CountDownLatch releaseOutbound = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> limiter.execute(() -> {
                outboundStarted.countDown();
                try {
                    if (!releaseOutbound.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release outbound request.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Outbound request was interrupted.", exception);
                }
                return null;
            }));

            assertThat(outboundStarted.await(2, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> limiter.execute(() -> "completed"));

            assertThat(second.get(1, TimeUnit.SECONDS)).isEqualTo("completed");
            releaseOutbound.countDown();
            first.get(1, TimeUnit.SECONDS);
        } finally {
            releaseOutbound.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void spavanjeNeBlokiraRezervacijuSljedecegTermina() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-19T08:00:00Z"));
        GeocodingProperties properties = properties(Duration.ofSeconds(1), Duration.ofSeconds(2));
        CountDownLatch firstSleepStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstSleep = new CountDownLatch(1);
        AtomicInteger sleepCount = new AtomicInteger();
        NominatimRateLimiter limiter = new NominatimRateLimiter(properties, clock, duration -> {
            if (sleepCount.getAndIncrement() == 0) {
                firstSleepStarted.countDown();
                if (!releaseFirstSleep.await(3, TimeUnit.SECONDS)) {
                    throw new InterruptedException("Timed out waiting to release the first sleep.");
                }
            } else {
                clock.advance(duration);
            }
        });
        var executor = Executors.newFixedThreadPool(2);

        try {
            limiter.execute(() -> null);
            var second = executor.submit(() -> limiter.execute(() -> "second"));
            assertThat(firstSleepStarted.await(2, TimeUnit.SECONDS)).isTrue();

            var third = executor.submit(() -> limiter.execute(() -> "third"));

            assertThat(third.get(1, TimeUnit.SECONDS)).isEqualTo("third");
            releaseFirstSleep.countDown();
            second.get(1, TimeUnit.SECONDS);
        } finally {
            releaseFirstSleep.countDown();
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
