package hr.fer.artconnect.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    /** Zona u kojoj aplikacija tumači datume i godine prema korisniku. */
    public static final ZoneId APP_ZONE = ZoneId.of("Europe/Zagreb");

    @Bean
    Clock zagrebClock() {
        return Clock.system(APP_ZONE);
    }
}
