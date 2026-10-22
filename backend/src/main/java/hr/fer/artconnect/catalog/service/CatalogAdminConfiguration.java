package hr.fer.artconnect.catalog.service;

import hr.fer.artconnect.catalog.domain.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogAdminConfiguration {
    @Bean
    CatalogAdminService<ArtField> artFieldAdminService(ArtFieldRepository repository) {
        return new CatalogAdminService<>(repository, ArtField::new);
    }

    @Bean
    CatalogAdminService<OpportunityType> opportunityTypeAdminService(OpportunityTypeRepository repository) {
        return new CatalogAdminService<>(repository, OpportunityType::new);
    }
}
