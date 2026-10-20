package hr.fer.artconnect.opportunity.service.geocoding;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NominatimRateLimiter {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Clock clock;
    private final Duration minInterval;
    private final Duration maxWait;
    private final Sleeper sleeper;
    private Instant lastRequestAt;

    @Autowired
    public NominatimRateLimiter(GeocodingProperties properties, Clock clock) {
        this(properties, clock, NominatimRateLimiter::sleep);
    }

    NominatimRateLimiter(GeocodingProperties properties, Clock clock, Sleeper sleeper) {
        this.clock = clock;
        this.minInterval = properties.getMinInterval();
        this.maxWait = properties.getMaxWait();
        this.sleeper = sleeper;
        if (minInterval == null || minInterval.isNegative() || maxWait == null || maxWait.isNegative()) {
            throw new IllegalArgumentException("Geocoding rate-limit durations must be non-negative.");
        }
    }

    public <T> T execute(Supplier<T> outboundRequest) {
        long waitNanos = maxWait.toNanos();
        long startedAt = System.nanoTime();
        boolean acquired;
        try {
            acquired = lock.tryLock(waitNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw rateLimited();
        }
        if (!acquired) {
            throw rateLimited();
        }

        try {
            while (lastRequestAt != null) {
                Instant allowedAt = lastRequestAt.plus(minInterval);
                Duration intervalRemaining = Duration.between(clock.instant(), allowedAt);
                if (intervalRemaining.isNegative() || intervalRemaining.isZero()) {
                    break;
                }
                long remainingWait = waitNanos - (System.nanoTime() - startedAt);
                if (remainingWait <= 0 || intervalRemaining.toNanos() > remainingWait) {
                    throw rateLimited();
                }
                sleeper.sleep(Duration.ofNanos(Math.min(intervalRemaining.toNanos(), remainingWait)));
            }
            lastRequestAt = clock.instant();
            return outboundRequest.get();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw rateLimited();
        } finally {
            lock.unlock();
        }
    }

    private static void sleep(Duration duration) throws InterruptedException {
        long millis = duration.toMillis();
        int nanos = (int) (duration.minusMillis(millis).toNanos());
        Thread.sleep(millis, nanos);
    }

    private static ApiException rateLimited() {
        return new ApiException(ErrorCode.RATE_LIMITED, "Previše zahtjeva za geokodiranje. Pokušajte ponovno.");
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }
}
